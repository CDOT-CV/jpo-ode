package us.dot.its.jpo.ode.udp.psm;

import java.net.DatagramPacket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import us.dot.its.jpo.ode.udp.AbstractUdpReceiverPublisher;
import us.dot.its.jpo.ode.udp.InvalidPayloadException;
import us.dot.its.jpo.ode.udp.UdpHexDecoder;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties.ReceiverProperties;

@Slf4j
public class PsmReceiver extends AbstractUdpReceiverPublisher {

    private final KafkaTemplate<String, String> psmPublisher;
    private final String publishTopic;

    public PsmReceiver(ReceiverProperties receiverProperties,
        KafkaTemplate<String, String> kafkaTemplate, String publishTopic) {
        super(receiverProperties.getReceiverPort(), receiverProperties.getBufferSize());

        this.publishTopic = publishTopic;
        this.psmPublisher = kafkaTemplate;
    }

    @Override
    public void run() {
        log.debug("PSM UDP Receiver Service started.");

        byte[] buffer = new byte[bufferSize];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        do {
            try {
                log.debug("Waiting for UDP PSM packets...");
                socket.receive(packet);
                if (packet.getLength() > 0) {
                    String psmJson = UdpHexDecoder.buildJsonPsmFromPacket(packet);
                    if (psmJson != null) {
                        psmPublisher.send(publishTopic, psmJson);
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
