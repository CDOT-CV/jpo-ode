package us.dot.its.jpo.ode.udp.bsm;

import java.net.DatagramPacket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import us.dot.its.jpo.ode.udp.AbstractUdpReceiverPublisher;
import us.dot.its.jpo.ode.udp.InvalidPayloadException;
import us.dot.its.jpo.ode.udp.UdpHexDecoder;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties.ReceiverProperties;

/**
 * The BsmReceiver class is responsible for receiving UDP packets containing Basic Safety Message
 * (BSM) data, decoding the packets, and publishing the decoded message to a specified Kafka topic.
 * It extends the AbstractUdpReceiverPublisher, leveraging its capabilities to receive UDP packets
 * asynchronously.
 */
@Slf4j
public class BsmReceiver extends AbstractUdpReceiverPublisher {

  private final KafkaTemplate<String, String> bsmPublisher;
  private final String publishTopic;

  /**
   * Constructs a BsmReceiver object that is responsible for receiving UDP packets, decoding Basic
   * Safety Message (BSM) data, and publishing the decoded message to a specified Kafka topic.
   *
   * @param receiverProperties The properties that configure the UDP receiver, including the port
   *                           and buffer size.
   * @param kafkaTemplate      The KafkaTemplate used for sending messages to the Kafka broker.
   * @param publishTopic       The Kafka topic to which the decoded BSM data should be published.
   */
  public BsmReceiver(ReceiverProperties receiverProperties,
      KafkaTemplate<String, String> kafkaTemplate, String publishTopic) {
    super(receiverProperties.getReceiverPort(), receiverProperties.getBufferSize());

    this.publishTopic = publishTopic;
    this.bsmPublisher = kafkaTemplate;
  }

  @Override
  public void run() {
    log.debug("BSM UDP Receiver Service started.");

    byte[] buffer = new byte[bufferSize];
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    do {
      try {
        log.info("Waiting for UDP BSM packets...");
        socket.receive(packet);
        if (packet.getLength() > 0) {
          String bsmData = UdpHexDecoder.buildJsonBsmFromPacket(packet);
          if (bsmData != null) {
            bsmPublisher.send(publishTopic, bsmData);
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
