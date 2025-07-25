package us.dot.its.jpo.ode.udp.rsm;

import java.net.DatagramPacket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import us.dot.its.jpo.ode.udp.AbstractUdpReceiverPublisher;
import us.dot.its.jpo.ode.udp.InvalidPayloadException;
import us.dot.its.jpo.ode.udp.UdpHexDecoder;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties.ReceiverProperties;

/**
 * The RsmReceiver class is responsible for receiving UDP packets containing Road Safety Message
 * (RSM) data, decoding the packets, and publishing the decoded message to a specified Kafka topic.
 * It extends the AbstractUdpReceiverPublisher, leveraging its capabilities to receive UDP packets
 * asynchronously.
 */
@Slf4j
public class RsmReceiver extends AbstractUdpReceiverPublisher {

  private final KafkaTemplate<String, String> rsmPublisher;
  private final String publishTopic;

  /**
   * Constructs a RsmReceiver object that is responsible for receiving UDP packets, decoding Road Safety Message
   * data, and publishing the decoded message to a specified Kafka topic.
   *
   * @param receiverProperties The properties that configure the UDP receiver, including the port
   *                           and buffer size.
   * @param kafkaTemplate      The KafkaTemplate used for sending messages to the Kafka broker.
   * @param publishTopic       The Kafka topic to which the decoded RSM data should be published.
   */
  public RsmReceiver(ReceiverProperties receiverProperties,
      KafkaTemplate<String, String> kafkaTemplate, String publishTopic) {
    super(receiverProperties.getReceiverPort(), receiverProperties.getBufferSize());

    this.publishTopic = publishTopic;
    this.rsmPublisher = kafkaTemplate;
  }

  @Override
  public void run() {
    log.debug("RSM UDP Receiver Service started.");

    byte[] buffer = new byte[bufferSize];
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    do {
      try {
        log.info("Waiting for UDP RSM packets...");
        socket.receive(packet);
        if (packet.getLength() > 0) {
          String rsmData = UdpHexDecoder.buildJsonRsmFromPacket(packet);
          if (rsmData != null) {
            rsmPublisher.send(publishTopic, rsmData);
          }
        }
      } catch (InvalidPayloadException e) {
        log.error("Error decoding packet", e);
      } catch (Exception e) {
        log.error("Error receiving packet", e);
      }
    } while (!isStopped());
  }
}
