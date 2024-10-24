package us.dot.its.jpo.ode.udp.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import us.dot.its.jpo.ode.OdePropertiesNew;
import us.dot.its.jpo.ode.coder.StringPublisher;
import us.dot.its.jpo.ode.udp.bsm.BsmReceiver;
import us.dot.its.jpo.ode.udp.generic.GenericReceiver;
import us.dot.its.jpo.ode.udp.tim.TimReceiver;
import us.dot.its.jpo.ode.udp.ssm.SsmReceiver;
import us.dot.its.jpo.ode.udp.srm.SrmReceiver;
import us.dot.its.jpo.ode.udp.spat.SpatReceiver;
import us.dot.its.jpo.ode.udp.map.MapReceiver;
import us.dot.its.jpo.ode.udp.psm.PsmReceiver;

/**
 * Centralized UDP service dispatcher.
 *
 */
@Controller
@Slf4j
public class UdpServicesController {

   @Autowired
   public UdpServicesController(OdePropertiesNew odeProps) {
      super();

      // Start the UDP receivers
      ServiceManager rm = new ServiceManager(new UdpServiceThreadFactory("UdpReceiverManager"));

      log.debug("Starting UDP receiver services...");

      // BSM internal
      rm.submit(new BsmReceiver(odeProps, odeKafkaProperties));

      // TIM internal
      rm.submit(new TimReceiver(odeProps, odeKafkaProperties));

      // SSM internal port
      rm.submit(new SsmReceiver(odeProps, odeKafkaProperties));
      
      // SRM internal port
      rm.submit(new SrmReceiver(odeProps, odeKafkaProperties));

      // SPAT internal port
      rm.submit(new SpatReceiver(odeProps, odeKafkaProperties));

      // MAP internal port
      rm.submit(new MapReceiver(odeProps, odeKafkaProperties));

      // PSM internal port
      rm.submit(new PsmReceiver(odeProps, odeKafkaProperties));

      // Generic Receiver internal port
      StringPublisher genericPublisher = new StringPublisher(odeKafkaProperties.getBrokers(), odeKafkaProperties.getProducerType(), odeKafkaProperties.getDisabledTopics());
      rm.submit(new GenericReceiver(odeProps, odeKafkaProperties));

      log.debug("UDP receiver services started.");
   }
}
