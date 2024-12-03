package us.dot.its.jpo.ode.udp.generic;

import io.netty.handler.codec.UnsupportedMessageTypeException;
import java.net.DatagramPacket;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.buf.HexUtils;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import us.dot.its.jpo.ode.kafka.topics.RawEncodedJsonTopics;
import us.dot.its.jpo.ode.udp.AbstractUdpReceiverPublisher;
import us.dot.its.jpo.ode.udp.InvalidPayloadException;
import us.dot.its.jpo.ode.udp.UdpHexDecoder;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties.ReceiverProperties;
import us.dot.its.jpo.ode.uper.UperUtil;

@Slf4j
public class GenericReceiver extends AbstractUdpReceiverPublisher {

  private final KafkaTemplate<String, String> publisher;
  private final RawEncodedJsonTopics rawEncodedJsonTopics;

  public GenericReceiver(ReceiverProperties props, KafkaTemplate<String, String> kafkaTemplate,
      RawEncodedJsonTopics rawEncodedJsonTopics) {
    super(props.getReceiverPort(), props.getBufferSize());

    this.publisher = kafkaTemplate;
    this.rawEncodedJsonTopics = rawEncodedJsonTopics;
  }

  @Override
  public void run() {
    log.debug("Generic UDP Receiver Service started.");

    byte[] buffer;
    do {
      buffer = new byte[bufferSize];
      // packet should be recreated on each loop to prevent latent data in buffer
      DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
      try {
        log.debug("Waiting for Generic UDP packets...");
        socket.receive(packet);
        byte[] payload = packet.getData();
        if ((packet.getLength() <= 0) || (payload == null)) {
          log.debug("Skipping empty payload");
          continue;
        }

        senderIp = packet.getAddress().getHostAddress();
        senderPort = packet.getPort();
        log.debug("Packet received from {}:{}", senderIp, senderPort);

        String payloadHexString = HexUtils.toHexString(payload).toLowerCase();
        log.debug("Raw Payload {}", payloadHexString);

        String messageType = UperUtil.determineHexPacketType(payloadHexString);

        log.debug("Detected Message Type {}", messageType);

        var completableFuture = routeMessageByMessageType(messageType, packet);
        completableFuture.whenCompleteAsync((message, exception) -> {
          if (exception != null) {
            log.error("Exception while publishing {} message", messageType, exception);
          }
        });

      } catch (UnsupportedMessageTypeException e) {
        log.error("Unsupported Message Type", e);
      } catch (InvalidPayloadException e) {
        log.error("Error decoding packet", e);
      } catch (Exception e) {
        log.error("Error receiving packet", e);
      }
    } while (!isStopped());
  }

  private CompletableFuture<SendResult<String, String>> routeMessageByMessageType(
      String messageType,
      DatagramPacket packet
  ) throws InvalidPayloadException, UnsupportedMessageTypeException {
    CompletableFuture<SendResult<String, String>> completableFuture = null;
    switch (messageType) {
      case "MAP" -> {
        String mapJson = UdpHexDecoder.buildJsonMapFromPacket(packet);
        log.debug("Sending Data to Topic {}", mapJson);
        if (mapJson != null) {
          completableFuture = publisher.send(rawEncodedJsonTopics.getMap(), mapJson);
        }
      }
      case "SPAT" -> {
        String spatJson = UdpHexDecoder.buildJsonSpatFromPacket(packet);
        if (spatJson != null) {
          completableFuture = publisher.send(rawEncodedJsonTopics.getSpat(), spatJson);
        }
      }
      case "TIM" -> {
        String timJson = UdpHexDecoder.buildJsonTimFromPacket(packet);
        if (timJson != null) {
          completableFuture = publisher.send(rawEncodedJsonTopics.getTim(), timJson);
        }
      }
      case "BSM" -> {
        String bsmJson = UdpHexDecoder.buildJsonBsmFromPacket(packet);
        if (bsmJson != null) {
          completableFuture = publisher.send(rawEncodedJsonTopics.getBsm(), bsmJson);
        }
      }
      case "SSM" -> {
        String ssmJson = UdpHexDecoder.buildJsonSsmFromPacket(packet);
        if (ssmJson != null) {
          completableFuture = publisher.send(rawEncodedJsonTopics.getSsm(), ssmJson);
        }
      }
      case "SRM" -> {
        String srmJson = UdpHexDecoder.buildJsonSrmFromPacket(packet);
        if (srmJson != null) {
          completableFuture = publisher.send(rawEncodedJsonTopics.getSrm(), srmJson);
        }
      }
      case "PSM" -> {
        String psmJson = UdpHexDecoder.buildJsonPsmFromPacket(packet);
        if (psmJson != null) {
          completableFuture = publisher.send(rawEncodedJsonTopics.getPsm(), psmJson);
        }
      }
      default -> throw new UnsupportedMessageTypeException(messageType);
    }
    return completableFuture;
  }
}
