package us.dot.its.jpo.ode.udp.bsm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(value = BSMReceiverProperties.class)
class BSMReceiverPropertiesTest {

    @Autowired
    BSMReceiverProperties bsmReceiverProperties;

    @Test
    void getPojoTopic() {
        assertEquals("topic.OdeBsmPojo", bsmReceiverProperties.getPojoTopic());
    }

    @Test
    void getJsonTopic() {
        assertEquals("topic.OdeBsmJson", bsmReceiverProperties.getJsonTopic());
    }

    @Test
    void getRxPojoTopic() {
        assertEquals("topic.OdeBsmRxPojo", bsmReceiverProperties.getRxPojoTopic());
    }

    @Test
    void getTxPojoTopic() {
        assertEquals("topic.OdeBsmTxPojo", bsmReceiverProperties.getTxPojoTopic());
    }

    @Test
    void getDuringEventPojoTopic() {
        assertEquals("topic.OdeBsmDuringEventPojo", bsmReceiverProperties.getDuringEventPojoTopic());
    }

    @Test
    void getFilteredJsonTopic() {
        assertEquals("topic.FilteredOdeBsmJson", bsmReceiverProperties.getFilteredJsonTopic());
    }

    @Test
    void getRawEncodedJsonTopic() {
        assertEquals("topic.OdeRawEncodedBSMJson", bsmReceiverProperties.getRawEncodedJsonTopic());
    }
}