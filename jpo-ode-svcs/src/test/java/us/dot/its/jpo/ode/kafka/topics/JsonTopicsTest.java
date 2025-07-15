package us.dot.its.jpo.ode.kafka.topics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(value = JsonTopics.class)
class JsonTopicsTest {

    @Autowired
    JsonTopics jsonTopics;

    @Test
    void getBsm() {
        assertEquals("topic.OdeBsmJson", jsonTopics.getBsm());
    }

    @Test
    void getMap() {
        assertEquals("topic.OdeMapJson", jsonTopics.getMap());
    }

    @Test
    void getPsm() {
        assertEquals("topic.OdePsmJson", jsonTopics.getPsm());
    }

    @Test
    void getSpat() {
        assertEquals("topic.OdeSpatJson", jsonTopics.getSpat());
    }

    @Test
    void getSrm() {
        assertEquals("topic.OdeSrmJson", jsonTopics.getSrm());
    }

    @Test
    void getSsm() {
        assertEquals("topic.OdeSsmJson", jsonTopics.getSsm());
    }

    @Test
    void getTim() {
        assertEquals("topic.OdeTimJson", jsonTopics.getTim());
    }

    @Test
    void getSdsm() {
        assertEquals("topic.OdeSdsmJson", jsonTopics.getSdsm());
    }

    @Test
    void getRtcm() {
        assertEquals("topic.OdeRtcmJson", jsonTopics.getRtcm());
    }

    @Test
    void getDriverAlert() {
        assertEquals("topic.OdeDriverAlertJson", jsonTopics.getDriverAlert());
    }

    @Test
    void getBsmFiltered() {
        assertEquals("topic.FilteredOdeBsmJson", jsonTopics.getBsmFiltered());
    }

    @Test
    void getSpatFiltered() {
        assertEquals("topic.FilteredOdeSpatJson", jsonTopics.getSpatFiltered());
    }

    @Test
    void getTimFiltered() {
        assertEquals("topic.FilteredOdeTimJson", jsonTopics.getTimFiltered());
    }

    @Test
    void getTimTmcFiltered() {
        assertEquals("topic.OdeTimJsonTMCFiltered", jsonTopics.getTimTmcFiltered());
    }

    @Test
    void getTimCertExpiration() {
        assertEquals("topic.OdeTIMCertExpirationTimeJson", jsonTopics.getTimCertExpiration());
    }
}