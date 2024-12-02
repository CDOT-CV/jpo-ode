package us.dot.its.jpo.ode.udp.bsm;

import java.net.DatagramPacket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import us.dot.its.jpo.ode.udp.AbstractUdpReceiverPublisher;
import us.dot.its.jpo.ode.udp.InvalidPayloadException;
import us.dot.its.jpo.ode.udp.UdpHexDecoder;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties;

@Slf4j
public class BsmReceiver extends AbstractUdpReceiverPublisher {

    private final KafkaTemplate<String, String> bsmPublisher;
    private final String publishTopic;

    public BsmReceiver(UDPReceiverProperties.ReceiverProperties receiverProperties, KafkaTemplate<String, String> template, String publishTopic) {
        super(receiverProperties.getReceiverPort(), receiverProperties.getBufferSize());

        this.publishTopic = publishTopic;
        this.bsmPublisher = template;
    }

    @Override
    public void run() {
        log.debug("BSM UDP Receiver Service started.");

        byte[] buffer = new byte[bufferSize];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        do {
            try {
                log.debug("Waiting for UDP BSM packets...");
                this.socket.receive(packet);
                if (packet.getLength() > 0) {
                    String bsmJson = UdpHexDecoder.buildJsonBsmFromPacket(packet);
                    if (bsmJson != null) {
                        log.debug("Publishing String data to {}", publishTopic);

                        var sendResult = bsmPublisher.send(publishTopic, bsmJson);
                        sendResult.whenCompleteAsync( (result, error) -> {
                            if (error != null) {
                                log.error("Error sending BSM to Kafka", error);
                            }
                        });
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
