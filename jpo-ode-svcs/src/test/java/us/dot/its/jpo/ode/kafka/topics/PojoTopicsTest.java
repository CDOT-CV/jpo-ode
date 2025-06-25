package us.dot.its.jpo.ode.kafka.topics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(value = PojoTopics.class)
class PojoTopicsTest {

  @Autowired
  PojoTopics pojoTopics;

  @Test
  void getSsm() {
    assertEquals("topic.OdeSsmPojo", pojoTopics.getSsm());
  }

  @Test
  void getTimBroadcast() {
    assertEquals("topic.OdeTimBroadcastPojo", pojoTopics.getTimBroadcast());
  }

  @Test
  void getTxPsm() {
    assertEquals("topic.OdePsmTxPojo", pojoTopics.getTxPsm());
  }
}
