package us.dot.its.jpo.ode.udp.rtcm;

import java.net.DatagramPacket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import us.dot.its.jpo.ode.udp.AbstractUdpReceiverPublisher;
import us.dot.its.jpo.ode.udp.InvalidPayloadException;
import us.dot.its.jpo.ode.udp.UdpHexDecoder;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties.ReceiverProperties;

/**
 * The RtcmReceiver class is responsible for receiving UDP packets containing RTCM data,
 * decoding the packets, and publishing the decoded message to a specified Kafka topic.
 * It extends the AbstractUdpReceiverPublisher, leveraging its capabilities to receive UDP packets
 * asynchronously.
 */
@Slf4j
public class RtcmReceiver extends AbstractUdpReceiverPublisher {
  private final KafkaTemplate<String, String> rtcmPublisher;
  private final String publishTopic;

  /**
   * Constructs a RtcmReceiver object that is responsible for receiving UDP packets, decoding RTCM
   * data, and publishing the decoded message to a specified Kafka topic.
   *
   * @param receiverProperties The properties that configure the UDP receiver, including the port
   *                           and buffer size.
   * @param kafkaTemplate      The KafkaTemplate used for sending messages to the Kafka broker.
   * @param publishTopic       The Kafka topic to which the decoded RTCM data should be published.
   */
  public RtcmReceiver(ReceiverProperties receiverProperties,
      KafkaTemplate<String, String> kafkaTemplate, String publishTopic) {
    super(receiverProperties.getReceiverPort(), receiverProperties.getBufferSize());

    this.publishTopic = publishTopic;
    this.rtcmPublisher = kafkaTemplate;
  }

  @Override
  public void run() {
    log.debug("RTCM UDP Receiver Service started.");

    byte[] buffer = new byte[bufferSize];
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    do {
      try {
        log.info("Waiting for UDP RTCM packets...");
        socket.receive(packet);
        if (packet.getLength() > 0) {
          String rtcmData = UdpHexDecoder.buildJsonRtcmFromPacket(packet);
          if (rtcmData != null) {
            rtcmPublisher.send(publishTopic, rtcmData);
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
