package us.dot.its.jpo.ode.udp.generic;

import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.buf.HexUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import us.dot.its.jpo.ode.OdeProperties;
import us.dot.its.jpo.ode.coder.StringPublisher;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.udp.AbstractUdpReceiverPublisher;
import us.dot.its.jpo.ode.udp.UdpHexDecoder;
import us.dot.its.jpo.ode.uper.UperUtil;

import java.net.DatagramPacket;

@Slf4j
public class GenericReceiver extends AbstractUdpReceiverPublisher {

    private final StringPublisher publisher;
    private final OdeKafkaProperties odeKafkaProperties;

    public GenericReceiver(@Qualifier("ode-us.dot.its.jpo.ode.OdeProperties") OdeProperties odeProps, OdeKafkaProperties odeKafkaProperties) {
        super(odeProps, odeProps.getGenericReceiverPort(), odeProps.getGenericBufferSize());

        this.odeKafkaProperties = odeKafkaProperties;
        this.publisher = new StringPublisher(odeProperties, odeKafkaProperties);
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

                switch (messageType) {
                    case "MAP" -> {
                        String mapJson = UdpHexDecoder.buildJsonMapFromPacket(packet);
                        log.debug("Sending Data to Topic {}", mapJson);
                        if (mapJson != null) {
                            publisher.publish(mapJson, publisher.getOdeProperties().getKafkaTopicOdeRawEncodedMAPJson());
                        }
                    }
                    case "SPAT" -> {
                        String spatJson = UdpHexDecoder.buildJsonSpatFromPacket(packet);
                        if (spatJson != null) {
                            publisher.publish(spatJson, publisher.getOdeProperties().getKafkaTopicOdeRawEncodedSPATJson());
                        }
                    }
                    case "TIM" -> {
                        String timJson = UdpHexDecoder.buildJsonTimFromPacket(packet);
                        if (timJson != null) {
                            publisher.publish(timJson, publisher.getOdeProperties().getKafkaTopicOdeRawEncodedTIMJson());
                        }
                    }
                    case "BSM" -> {
                        String bsmJson = UdpHexDecoder.buildJsonBsmFromPacket(packet);
                        if (bsmJson != null) {
                            publisher.publish(bsmJson, this.odeKafkaProperties.getBsmProperties().getRawEncodedJsonTopic());
                        }
                    }
                    case "SSM" -> {
                        String ssmJson = UdpHexDecoder.buildJsonSsmFromPacket(packet);
                        if (ssmJson != null) {
                            publisher.publish(ssmJson, this.odeProperties.getKafkaTopicOdeRawEncodedSSMJson());
                        }
                    }
                    case "SRM" -> {
                        String srmJson = UdpHexDecoder.buildJsonSrmFromPacket(packet);
                        if (srmJson != null) {
                            publisher.publish(srmJson, this.odeProperties.getKafkaTopicOdeRawEncodedSRMJson());
                        }
                    }
                    case "PSM" -> {
                        String psmJson = UdpHexDecoder.buildJsonPsmFromPacket(packet);
                        if (psmJson != null) {
                            publisher.publish(psmJson, this.odeProperties.getKafkaTopicOdeRawEncodedPSMJson());
                        }
                    }
                    default -> log.debug("Unknown Message Type");
                }
            } catch (Exception e) {
                log.error("Error receiving packet", e);
            }
        } while (!isStopped());
    }
}
