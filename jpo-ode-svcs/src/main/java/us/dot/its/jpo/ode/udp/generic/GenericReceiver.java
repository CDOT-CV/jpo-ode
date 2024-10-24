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

    private GenericReceiverProperties props;

    public GenericReceiver(@Qualifier("ode-us.dot.its.jpo.ode.OdeProperties") OdeProperties odeProps, OdeKafkaProperties odeKafkaProperties) {
        super(odeProps, odeProps.getGenericReceiverPort(), odeProps.getGenericBufferSize());

        this.publisher = new StringPublisher(odeKafkaProperties.getBrokers(), odeKafkaProperties.getProducerType(), odeKafkaProperties.getDisabledTopics());
    }

    public GenericReceiver(GenericReceiverProperties props, StringPublisher publisher) {
        super(props.getReceiverPort(), props.getBufferSize());

        this.publisher = publisher;
        this.props = props;
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
                            publisher.publish(props.getRawEncodedJsonTopics().getMap(), mapJson);
                        }
                    }
                    case "SPAT" -> {
                        String spatJson = UdpHexDecoder.buildJsonSpatFromPacket(packet);
                        if (spatJson != null) {
                            publisher.publish(props.getRawEncodedJsonTopics().getSpat(), spatJson);
                        }
                    }
                    case "TIM" -> {
                        String timJson = UdpHexDecoder.buildJsonTimFromPacket(packet);
                        if (timJson != null) {
                            publisher.publish(props.getRawEncodedJsonTopics().getTim(), timJson);
                        }
                    }
                    case "BSM" -> {
                        String bsmJson = UdpHexDecoder.buildJsonBsmFromPacket(packet);
                        if (bsmJson != null) {
                            publisher.publish(props.getRawEncodedJsonTopics().getBsm(), bsmJson);
                        }
                    }
                    case "SSM" -> {
                        String ssmJson = UdpHexDecoder.buildJsonSsmFromPacket(packet);
                        if (ssmJson != null) {
                            publisher.publish(props.getRawEncodedJsonTopics().getSsm(), ssmJson);
                        }
                    }
                    case "SRM" -> {
                        String srmJson = UdpHexDecoder.buildJsonSrmFromPacket(packet);
                        if (srmJson != null) {
                            publisher.publish(props.getRawEncodedJsonTopics().getSrm(), srmJson);
                        }
                    }
                    case "PSM" -> {
                        String psmJson = UdpHexDecoder.buildJsonPsmFromPacket(packet);
                        if (psmJson != null) {
                            publisher.publish(props.getRawEncodedJsonTopics().getPsm(), psmJson);
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
