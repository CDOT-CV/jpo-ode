package us.dot.its.jpo.ode.udp.tim;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import us.dot.its.jpo.ode.udp.AbstractUdpReceiverPublisher;
import us.dot.its.jpo.ode.udp.InvalidPayloadException;
import us.dot.its.jpo.ode.udp.UdpHexDecoder;

import java.net.DatagramPacket;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties.ReceiverProperties;

@Slf4j
public class TimReceiver extends AbstractUdpReceiverPublisher {

  private final KafkaTemplate<String, String> timPublisher;
  private final String publishTopic;

  public TimReceiver(ReceiverProperties receiverProperties,
      KafkaTemplate<String, String> kafkaTemplate, String publishTopic) {
    super(receiverProperties.getReceiverPort(), receiverProperties.getBufferSize());

    this.publishTopic = publishTopic;
    this.timPublisher = kafkaTemplate;
  }

  @Override
  public void run() {
    log.debug("TIM UDP Receiver Service started.");

    byte[] buffer = new byte[bufferSize];

    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

    do {
      try {
        log.debug("Waiting for UDP TIM packets...");
        socket.receive(packet);
        if (packet.getLength() > 0) {

          String timJson = UdpHexDecoder.buildJsonTimFromPacket(packet);
          if (timJson != null) {
            timPublisher.send(publishTopic, timJson);
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
