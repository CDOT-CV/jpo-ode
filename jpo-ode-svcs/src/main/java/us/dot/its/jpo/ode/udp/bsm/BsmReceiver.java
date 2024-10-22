package us.dot.its.jpo.ode.udp.bsm;

import java.net.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import us.dot.its.jpo.ode.ODEKafkaProperties;
import us.dot.its.jpo.ode.OdeProperties;
import us.dot.its.jpo.ode.coder.StringPublisher;
import us.dot.its.jpo.ode.udp.AbstractUdpReceiverPublisher;
import us.dot.its.jpo.ode.udp.UdpHexDecoder;

public class BsmReceiver extends AbstractUdpReceiverPublisher {

   private static Logger logger = LoggerFactory.getLogger(BsmReceiver.class);

   private final StringPublisher bsmPublisher;

   @Autowired
   public BsmReceiver(@Qualifier("ode-us.dot.its.jpo.ode.OdeProperties") OdeProperties odeProps, ODEKafkaProperties odeKafkaProperties) {
      this(odeProps, odeKafkaProperties, odeProps.getBsmReceiverPort(), odeProps.getBsmBufferSize());
   }

   public BsmReceiver(OdeProperties odeProps, ODEKafkaProperties odeKafkaProperties, int port, int bufferSize) {
      super(odeProps, port, bufferSize);

      this.bsmPublisher = new StringPublisher(odeProperties, odeKafkaProperties);
   }

   @Override
   public void run() {

      logger.debug("BSM UDP Receiver Service started.");

      byte[] buffer = new byte[bufferSize];

      DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

      do {
         try {
            logger.debug("Waiting for UDP BSM packets...");
            this.socket.receive(packet);
            if (packet.getLength() > 0) {
               String bsmJson = UdpHexDecoder.buildJsonBsmFromPacket(packet);

               if(bsmJson != null){
                  bsmPublisher.publish(bsmJson, bsmPublisher.getOdeProperties().getKafkaTopicOdeRawEncodedBSMJson());
               }
               
               
            }
         } catch (Exception e) {
            logger.error("Error receiving packet", e);
         }
      } while (!isStopped());
   }

   
}
