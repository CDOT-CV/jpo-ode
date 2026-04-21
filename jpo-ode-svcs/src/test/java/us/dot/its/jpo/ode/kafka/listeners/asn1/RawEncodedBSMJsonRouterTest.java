package us.dot.its.jpo.ode.kafka.listeners.asn1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import us.dot.its.jpo.ode.config.SerializationConfig;
import us.dot.its.jpo.ode.kafka.KafkaConsumerConfig;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.kafka.TestMetricsConfig;
import us.dot.its.jpo.ode.kafka.listeners.json.RawEncodedBSMJsonRouter;
import us.dot.its.jpo.ode.kafka.listeners.json.RawEncodedJsonService;
import us.dot.its.jpo.ode.kafka.producer.KafkaProducerConfig;
import us.dot.its.jpo.ode.kafka.topics.RawEncodedJsonTopics;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    classes = { KafkaProducerConfig.class, KafkaConsumerConfig.class, RawEncodedBSMJsonRouter.class,
        RawEncodedJsonService.class, SerializationConfig.class, TestMetricsConfig.class, },
    properties = {"ode.kafka.topics.raw-encoded-json.bsm=topic.Asn1DecoderTestBSMJSON",
        "ode.kafka.topics.asn1.decoder-input=topic.Asn1DecoderBSMInput"})
@EmbeddedKafka
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EnableConfigurationProperties
@ContextConfiguration(classes = {UDPReceiverProperties.class, OdeKafkaProperties.class,
    RawEncodedJsonTopics.class, KafkaProperties.class})
@DirtiesContext
class RawEncodedBSMJsonRouterTest {

  @Value(value = "${ode.kafka.topics.raw-encoded-json.bsm}")
  private String rawEncodedBsmJson;

  @Value(value = "${ode.kafka.topics.asn1.decoder-input}")
  private String asn1DecoderInput;

  @Autowired
  KafkaTemplate<String, String> kafkaTemplate;

  private final CountDownLatch latch = new CountDownLatch(1);
  private String odeBsmData;

  @Test
  void testListen() throws JSONException, IOException, InterruptedException {

    var classLoader = getClass().getClassLoader();
    InputStream inputStream = classLoader
        .getResourceAsStream("us/dot/its/jpo/ode/kafka/listeners/asn1/decoder-input-bsm.json");
    assert inputStream != null;
    var bsmJson = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    kafkaTemplate.send(rawEncodedBsmJson, bsmJson);

    inputStream =
        classLoader.getResourceAsStream("us/dot/its/jpo/ode/kafka/listeners/asn1/expected-bsm.xml");
    assert inputStream != null;
    var expectedBsm = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

    assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

    assertEquals(expectedBsm, odeBsmData);
  }

    @KafkaListener(topics = {"topic.Asn1DecoderBSMInput"} , groupId = "test-group")
    public void receive(String payload) {
        odeBsmData = payload;
        latch.countDown();
    }
}