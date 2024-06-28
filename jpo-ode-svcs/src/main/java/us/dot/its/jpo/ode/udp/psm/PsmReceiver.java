package us.dot.its.jpo.ode.udp.psm;

import java.net.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import us.dot.its.jpo.ode.coder.StringPublisher;
import us.dot.its.jpo.ode.OdeProperties;
import us.dot.its.jpo.ode.udp.AbstractUdpReceiverPublisher;
import us.dot.its.jpo.ode.udp.UdpHexDecoder;

public class PsmReceiver extends AbstractUdpReceiverPublisher {
    private static Logger logger = LoggerFactory.getLogger(PsmReceiver.class);

    private StringPublisher psmPublisher;

    @Autowired
    public PsmReceiver(OdeProperties odeProps) {
        this(odeProps, odeProps.getPsmReceiverPort(), odeProps.getPsmBufferSize());

        this.psmPublisher = new StringPublisher(odeProps);
    }

    public PsmReceiver(OdeProperties odeProps, int port, int bufferSize) {
        super(odeProps, port, bufferSize);

        this.psmPublisher = new StringPublisher(odeProps);
    }

    @Override
    public void run() {

        logger.debug("PSM UDP Receiver Service started.");

        byte[] buffer = new byte[bufferSize];

        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        do {
            try {
                logger.debug("Waiting for UDP PSM packets...");
                socket.receive(packet);
                if (packet.getLength() > 0) {
                    String psmJson = UdpHexDecoder.buildJsonPsmFromPacket(packet);
                    if(psmJson != null){
                        // Submit JSON to the OdeRawEncodedMessageJson Kafka Topic
                        psmPublisher.publish(psmJson, psmPublisher.getOdeProperties().getKafkaTopicOdeRawEncodedPSMJson());
                    }
                }
            } catch (Exception e) {
                logger.error("Error receiving packet", e);
            }
        } while (!isStopped());
    }

    
}
