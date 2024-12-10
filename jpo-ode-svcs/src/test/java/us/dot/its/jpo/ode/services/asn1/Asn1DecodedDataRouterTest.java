package us.dot.its.jpo.ode.services.asn1;

import static org.junit.jupiter.api.Assertions.fail;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.kafka.topics.RawEncodedJsonTopics;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties;

@Slf4j
@SpringBootTest()
@EnableConfigurationProperties
@ContextConfiguration(classes = {
    UDPReceiverProperties.class, OdeKafkaProperties.class,
    RawEncodedJsonTopics.class, KafkaProperties.class
})
@DirtiesContext
class Asn1DecodedDataRouterTest {

  @Test
  void testAsn1DecodedDataRouter_BSMDataFlow() {
    fail("Not yet implemented");
  }

  @Test
  void testAsn1DecodedDataRouter_TIMDataFlow() {
    fail("Not yet implemented");
  }

  @Test
  void testAsn1DecodedDataRouter_SPaTDataFlow() {
    fail("Not yet implemented");
  }

  @Test
  void testAsn1DecodedDataRouter_SSMDataFlow() {
    fail("Not yet implemented");
  }

  @Test
  void testAsn1DecodedDataRouter_SRMDataFlow() {
    fail("Not yet implemented");
  }

  @Test
  void testAsn1DecodedDataRouter_PSMDataFlow() {
    fail("Not yet implemented");
  }

  @Test
  void testAsn1DecodedDataRouter_MAPDataFlow() {
    fail("Not yet implemented");
  }
}
