package us.dot.its.jpo.ode.udp.bsm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(value = BSMProperties.class)
class BSMPropertiesTest {

    @Autowired
    BSMProperties bsmProperties;

    @Test
    void getPojoTopic() {
        assertEquals("topic.OdeBsmPojo", bsmProperties.getPojoTopic());
    }

    @Test
    void getJsonTopic() {
        assertEquals("topic.OdeBsmJson", bsmProperties.getJsonTopic());
    }

    @Test
    void getRxPojoTopic() {
        assertEquals("topic.OdeBsmRxPojo", bsmProperties.getRxPojoTopic());
    }

    @Test
    void getTxPojoTopic() {
        assertEquals("topic.OdeBsmTxPojo", bsmProperties.getTxPojoTopic());
    }

    @Test
    void getDuringEventPojoTopic() {
        assertEquals("topic.OdeBsmDuringEventPojo", bsmProperties.getDuringEventPojoTopic());
    }

    @Test
    void getFilteredJsonTopic() {
        assertEquals("topic.FilteredOdeBsmJson", bsmProperties.getFilteredJsonTopic());
    }

    @Test
    void getRawEncodedJsonTopic() {
        assertEquals("topic.OdeRawEncodedBSMJson", bsmProperties.getRawEncodedJsonTopic());
    }
}