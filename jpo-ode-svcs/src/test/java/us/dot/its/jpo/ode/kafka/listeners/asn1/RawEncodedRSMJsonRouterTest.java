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
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import us.dot.its.jpo.ode.config.SerializationConfig;
import us.dot.its.jpo.ode.kafka.KafkaConsumerConfig;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.kafka.TestMetricsConfig;
import us.dot.its.jpo.ode.kafka.listeners.json.RawEncodedJsonService;
import us.dot.its.jpo.ode.kafka.listeners.json.RawEncodedRSMJsonRouter;
import us.dot.its.jpo.ode.kafka.producer.KafkaProducerConfig;
import us.dot.its.jpo.ode.kafka.topics.Asn1CoderTopics;
import us.dot.its.jpo.ode.kafka.topics.RawEncodedJsonTopics;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties;

/**
 * Unit test for {@link RawEncodedRSMJsonRouter}.
 * Tests routing of RSM JSON messages to the appropriate Kafka topic.
 */
@SpringBootTest(
    classes = {
        KafkaProducerConfig.class,
        KafkaConsumerConfig.class,
        RawEncodedRSMJsonRouter.class,
        RawEncodedJsonService.class,
        SerializationConfig.class,
        TestMetricsConfig.class,
        UDPReceiverProperties.class,
        OdeKafkaProperties.class,
        RawEncodedJsonTopics.class,
        KafkaProperties.class,
        Asn1CoderTopics.class
    },
    properties = {
        "ode.kafka.topics.raw-encoded-json.rsm=topic.Asn1DecoderTestRSMJSON",
        "ode.kafka.topics.asn1.decoder-input=topic.Asn1DecoderRSMInput"
    })
@EmbeddedKafka
@TestPropertySource(properties = {"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"})
@EnableConfigurationProperties
@DirtiesContext
public class RawEncodedRSMJsonRouterTest {

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
            "us/dot/its/jpo/ode/kafka/listeners/asn1/decoder-input-rsm.json");
    assert inputStream != null;
    var json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

    inputStream = classLoader
        .getResourceAsStream("us/dot/its/jpo/ode/kafka/listeners/asn1/expected-rsm.xml");
    assert inputStream != null;
    var expectedRSM = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

    kafkaTemplate.send(rawEncodedJsonTopics.getRsm(), json);

    assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    assertEquals(expectedRSM, actualPayload);
  }

  @KafkaListener(topics = "topic.Asn1DecoderRSMInput")
  public void receive(String payload) {
    this.actualPayload = payload;
    latch.countDown();
  }
}