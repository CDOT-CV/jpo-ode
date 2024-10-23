package us.dot.its.jpo.ode.udp.bsm;

import java.net.DatagramPacket;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.OdeProperties;
import us.dot.its.jpo.ode.coder.StringPublisher;
import us.dot.its.jpo.ode.udp.AbstractUdpReceiverPublisher;
import us.dot.its.jpo.ode.udp.UdpHexDecoder;

@Slf4j
public class BsmReceiver extends AbstractUdpReceiverPublisher {

   private final StringPublisher bsmPublisher;

   public BsmReceiver(@Qualifier("ode-us.dot.its.jpo.ode.OdeProperties") OdeProperties odeProps, OdeKafkaProperties odeKafkaProperties) {
      super(odeProps, odeKafkaProperties.getBsmProperties().getReceiverPort(), odeKafkaProperties.getBsmProperties().getBufferSize());

      this.bsmPublisher = new StringPublisher(odeProperties, odeKafkaProperties);
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

               if (bsmJson != null){
                  bsmPublisher.publish(bsmJson, bsmPublisher.getOdeKafkaProperties().getBsmProperties().getRawEncodedJsonTopic());
               }
            }
         } catch (Exception e) {
            log.error("Error receiving packet", e);
         }
      } while (!isStopped());
   }

   
}
