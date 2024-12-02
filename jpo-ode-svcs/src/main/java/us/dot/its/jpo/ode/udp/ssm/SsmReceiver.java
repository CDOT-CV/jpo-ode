package us.dot.its.jpo.ode.udp.ssm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import us.dot.its.jpo.ode.udp.AbstractUdpReceiverPublisher;
import us.dot.its.jpo.ode.udp.InvalidPayloadException;
import us.dot.its.jpo.ode.udp.UdpHexDecoder;

import java.net.DatagramPacket;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties.ReceiverProperties;

@Slf4j
public class SsmReceiver extends AbstractUdpReceiverPublisher {

    private final KafkaTemplate<String, String> ssmPublisher;
    private final String publishTopic;

    public SsmReceiver(ReceiverProperties receiverProperties,
        KafkaTemplate<String, String> kafkaTemplate, String publishTopic) {
        super(receiverProperties.getReceiverPort(), receiverProperties.getBufferSize());

        this.publishTopic = publishTopic;
        this.ssmPublisher = kafkaTemplate;
    }

    @Override
    public void run() {
        log.debug("SSM UDP Receiver Service started.");

        byte[] buffer = new byte[bufferSize];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        do {
            try {
                log.debug("Waiting for UDP SSM packets...");
                socket.receive(packet);
                if (packet.getLength() > 0) {
                    String ssmJson = UdpHexDecoder.buildJsonSsmFromPacket(packet);
                    if (ssmJson != null) {
                        ssmPublisher.send(publishTopic, ssmJson);
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
