package us.dot.its.jpo.ode.kafka.listeners.asn1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
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
import us.dot.its.jpo.ode.kafka.listeners.json.RawEncodedJsonService;
import us.dot.its.jpo.ode.kafka.listeners.json.RawEncodedPSMJsonRouter;
import us.dot.its.jpo.ode.kafka.producer.KafkaProducerConfig;
import us.dot.its.jpo.ode.kafka.topics.Asn1CoderTopics;
import us.dot.its.jpo.ode.kafka.topics.RawEncodedJsonTopics;

@SpringBootTest(
    classes = {
        OdeKafkaProperties.class,
        KafkaProducerConfig.class,
        KafkaConsumerConfig.class,
        KafkaProperties.class,
        RawEncodedPSMJsonRouter.class,
        RawEncodedJsonService.class,
        SerializationConfig.class,
        TestMetricsConfig.class,
    },
    properties = {
        "ode.kafka.topics.raw-encoded-json.psm=topic.Asn1DecoderTestPSMJSON",
        "ode.kafka.topics.asn1.decoder-input=topic.Asn1DecoderPSMInput"
    })
@EmbeddedKafka(partitions = 1, topics = {"topic.Asn1DecoderTestPSMJSON", "topic.Asn1DecoderPSMInput"})
@TestPropertySource(properties = {"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"})
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(value = {
    OdeKafkaProperties.class,
    Asn1CoderTopics.class,
    RawEncodedJsonTopics.class})
@DirtiesContext
class RawEncodedPSMJsonRouterTest {

  @Autowired
  Asn1CoderTopics asn1CoderTopics;
  @Autowired
  RawEncodedJsonTopics rawEncodedJsonTopics;
  @Autowired
  private KafkaTemplate<String, String> kafkaTemplate;

  private CountDownLatch latch;
  private String actualPayload;

  @Test
  void testListen() throws JSONException, IOException, InterruptedException {

    latch = new CountDownLatch(1);
    actualPayload = null;

    var classLoader = getClass().getClassLoader();
    InputStream inputStream = classLoader
        .getResourceAsStream(
            "us/dot/its/jpo/ode/kafka/listeners/asn1/decoder-input-psm.json");
    assert inputStream != null;
    var psmJson = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

    inputStream = classLoader
        .getResourceAsStream("us/dot/its/jpo/ode/kafka/listeners/asn1/expected-psm.xml");
    assert inputStream != null;
    var expectedPsm = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

    kafkaTemplate.send(rawEncodedJsonTopics.getPsm(), psmJson);

    assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

    assertEquals(expectedPsm, actualPayload);
  }

  @KafkaListener(topics = "topic.Asn1DecoderPSMInput")
    public void receive(String payload) {
      this.actualPayload = payload;
      latch.countDown();
  }
}