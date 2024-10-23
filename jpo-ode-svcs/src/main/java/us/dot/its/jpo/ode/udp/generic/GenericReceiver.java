package us.dot.its.jpo.ode.udp.generic;

import java.net.DatagramPacket;
import org.apache.tomcat.util.buf.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Qualifier;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.OdeProperties;
import us.dot.its.jpo.ode.coder.StringPublisher;
import us.dot.its.jpo.ode.udp.AbstractUdpReceiverPublisher;
import us.dot.its.jpo.ode.udp.UdpHexDecoder;
import us.dot.its.jpo.ode.uper.UperUtil;

public class GenericReceiver extends AbstractUdpReceiverPublisher {

    private static Logger logger = LoggerFactory.getLogger(GenericReceiver.class);

    private final StringPublisher publisher;
    private final OdeKafkaProperties odeKafkaProperties;

    public GenericReceiver(@Qualifier("ode-us.dot.its.jpo.ode.OdeProperties") OdeProperties odeProps, OdeKafkaProperties odeKafkaProperties) {
        super(odeProps, odeProps.getGenericReceiverPort(), odeProps.getGenericBufferSize());

        this.odeKafkaProperties = odeKafkaProperties;
        this.publisher = new StringPublisher(odeProperties, odeKafkaProperties);
    }

    @Override
    public void run() {

        logger.debug("Generic UDP Receiver Service started.");

        byte[] buffer;

       

        do {
	    buffer = new byte[bufferSize];
            // packet should be recreated on each loop to prevent latent data in buffer
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                logger.debug("Waiting for Generic UDP packets...");
                socket.receive(packet);
                if (packet.getLength() > 0) {
                    senderIp = packet.getAddress().getHostAddress();
                    senderPort = packet.getPort();
                    logger.debug("Packet received from {}:{}", senderIp, senderPort);

                    byte[] payload = packet.getData();
                    if (payload == null){
                        logger.debug("Skipping Null Payload");
                        continue;
                    }
                    String payloadHexString = HexUtils.toHexString(payload).toLowerCase();
                    logger.debug("Raw Payload" + payloadHexString);
		    
		    String messageType = UperUtil.determineHexPacketType(payloadHexString);

                    logger.debug("Detected Message Type {}", messageType);

                    if (messageType.equals("MAP")) {
                        String mapJson = UdpHexDecoder.buildJsonMapFromPacket(packet);
			logger.debug("Sending Data to Topic" + mapJson);
                        if(mapJson != null){
                            publisher.publish(mapJson, publisher.getOdeProperties().getKafkaTopicOdeRawEncodedMAPJson());
                        }
                    } else if(messageType.equals("SPAT")) {
                        String spatJson = UdpHexDecoder.buildJsonSpatFromPacket(packet);
                        if(spatJson != null){
                            publisher.publish(spatJson, publisher.getOdeProperties().getKafkaTopicOdeRawEncodedSPATJson());
                        }
                    } else if (messageType.equals("TIM")) {
                        String timJson = UdpHexDecoder.buildJsonTimFromPacket(packet);
                        if(timJson != null){
                            publisher.publish(timJson, publisher.getOdeProperties().getKafkaTopicOdeRawEncodedTIMJson());
                        }
                    } else if (messageType.equals("BSM")) {
                        String bsmJson = UdpHexDecoder.buildJsonBsmFromPacket(packet);
                        if(bsmJson!=null){
                            publisher.publish(bsmJson, this.odeKafkaProperties.getBsmProperties().getRawEncodedJsonTopic());
                        }
                    } else if (messageType.equals("SSM")) {
                        String ssmJson = UdpHexDecoder.buildJsonSsmFromPacket(packet);
                        if(ssmJson!=null){
                            publisher.publish(ssmJson, this.odeProperties.getKafkaTopicOdeRawEncodedSSMJson());
                        }
                    } else if (messageType.equals("SRM")) {
                        String srmJson = UdpHexDecoder.buildJsonSrmFromPacket(packet);
                        if(srmJson!=null){
                            publisher.publish(srmJson, this.odeProperties.getKafkaTopicOdeRawEncodedSRMJson());
                        }
                    } else if (messageType.equals("PSM")) {
                        String psmJson = UdpHexDecoder.buildJsonPsmFromPacket(packet);
                        if(psmJson!=null){
                            publisher.publish(psmJson, this.odeProperties.getKafkaTopicOdeRawEncodedPSMJson());
                        }
                    }else{
                        logger.debug("Unknown Message Type");
                    }
                }
            } catch (Exception e) {
                logger.error("Error receiving packet", e);
            }
        } while (!isStopped());
   }
}
