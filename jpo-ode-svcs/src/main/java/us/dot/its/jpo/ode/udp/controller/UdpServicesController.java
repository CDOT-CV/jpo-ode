package us.dot.its.jpo.ode.udp.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.kafka.RawEncodedJsonTopics;
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

    @Autowired
    public UdpServicesController(UDPReceiverProperties udpProps, OdeKafkaProperties odeKafkaProperties, RawEncodedJsonTopics rawEncodedJsonTopics) {
        super();

        // Start the UDP receivers
        ServiceManager rm = new ServiceManager(new UdpServiceThreadFactory("UdpReceiverManager"));

        log.debug("Starting UDP receiver services...");

        // BSM internal
        rm.submit(new BsmReceiver(udpProps.getBsm(), odeKafkaProperties, rawEncodedJsonTopics.getBsm()));

        // TIM internal
        rm.submit(new TimReceiver(udpProps.getTim(), odeKafkaProperties, rawEncodedJsonTopics.getTim()));

        // SSM internal port
        rm.submit(new SsmReceiver(udpProps.getSsm(), odeKafkaProperties, rawEncodedJsonTopics.getSsm()));

        // SRM internal port
        rm.submit(new SrmReceiver(udpProps.getSrm(), odeKafkaProperties, rawEncodedJsonTopics.getSrm()));

        // SPAT internal port
        rm.submit(new SpatReceiver(udpProps.getSpat(), odeKafkaProperties, rawEncodedJsonTopics.getSpat()));

        // MAP internal port
        rm.submit(new MapReceiver(udpProps.getMap(), odeKafkaProperties, rawEncodedJsonTopics.getMap()));

        // PSM internal port
        rm.submit(new PsmReceiver(udpProps.getPsm(), odeKafkaProperties, rawEncodedJsonTopics.getPsm()));

        // Generic Receiver internal port
        rm.submit(new GenericReceiver(udpProps.getGeneric(), odeKafkaProperties, rawEncodedJsonTopics));

        log.debug("UDP receiver services started.");
    }
}
