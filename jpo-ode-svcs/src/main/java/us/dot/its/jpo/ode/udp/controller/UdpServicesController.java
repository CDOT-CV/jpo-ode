package us.dot.its.jpo.ode.udp.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Controller;
import us.dot.its.jpo.ode.kafka.topics.RawEncodedJsonTopics;
import us.dot.its.jpo.ode.udp.bsm.BsmReceiver;
import us.dot.its.jpo.ode.udp.generic.GenericReceiver;
import us.dot.its.jpo.ode.udp.map.MapReceiver;
import us.dot.its.jpo.ode.udp.psm.PsmReceiver;
import us.dot.its.jpo.ode.udp.spat.SpatReceiver;
import us.dot.its.jpo.ode.udp.srm.SrmReceiver;
import us.dot.its.jpo.ode.udp.ssm.SsmReceiver;
import us.dot.its.jpo.ode.udp.tim.TimReceiver;

/**
 * Centralized UDP service dispatcher.
 */
@Controller
@Slf4j
public class UdpServicesController {

  /**
   * Constructs a UdpServicesController to manage UDP receiver services for different message types.
   *
   * @param udpProps Properties containing configuration for each UDP receiver.
   * @param rawEncodedJsonTopics Topics to which the decoded messages will be published via Kafka.
   * @param kafkaTemplate Template to facilitate sending messages to Kafka topics.
   */
  @Autowired
  public UdpServicesController(UDPReceiverProperties udpProps,
      RawEncodedJsonTopics rawEncodedJsonTopics,
      KafkaTemplate<String, String> kafkaTemplate) {
    super();

    ServiceManager rm = new ServiceManager(new UdpServiceThreadFactory("UdpReceiverManager"));
    log.debug("Starting UDP receiver services...");

    rm.submit(new BsmReceiver(udpProps.getBsm(), kafkaTemplate, rawEncodedJsonTopics.getBsm()));
    rm.submit(new TimReceiver(udpProps.getTim(), kafkaTemplate, rawEncodedJsonTopics.getTim()));
    rm.submit(new SsmReceiver(udpProps.getSsm(), kafkaTemplate, rawEncodedJsonTopics.getSsm()));
    rm.submit(new SrmReceiver(udpProps.getSrm(), kafkaTemplate, rawEncodedJsonTopics.getSrm()));
    rm.submit(new SpatReceiver(udpProps.getSpat(), kafkaTemplate, rawEncodedJsonTopics.getSpat()));
    rm.submit(new MapReceiver(udpProps.getMap(), kafkaTemplate, rawEncodedJsonTopics.getMap()));
    rm.submit(new PsmReceiver(udpProps.getPsm(), kafkaTemplate, rawEncodedJsonTopics.getPsm()));
    rm.submit(new GenericReceiver(udpProps.getGeneric(), kafkaTemplate, rawEncodedJsonTopics));

    log.debug("UDP receiver services started.");
  }
}
