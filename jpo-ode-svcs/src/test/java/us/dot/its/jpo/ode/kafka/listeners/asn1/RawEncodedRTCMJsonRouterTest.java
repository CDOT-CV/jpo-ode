package us.dot.its.jpo.ode.kafka.listeners.asn1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import us.dot.its.jpo.ode.config.SerializationConfig;
import us.dot.its.jpo.ode.kafka.KafkaConsumerConfig;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.kafka.TestMetricsConfig;
import us.dot.its.jpo.ode.kafka.listeners.json.RawEncodedJsonService;
import us.dot.its.jpo.ode.kafka.listeners.json.RawEncodedRTCMJsonRouter;
import us.dot.its.jpo.ode.kafka.producer.KafkaProducerConfig;
import us.dot.its.jpo.ode.kafka.topics.Asn1CoderTopics;
import us.dot.its.jpo.ode.kafka.topics.RawEncodedJsonTopics;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties;

/**
 * Unit test for {@link RawEncodedRTCMJsonRouter}.
 * Tests routing of RTCM JSON messages to the appropriate Kafka topic.
 */
@SpringBootTest(
    classes = {
        KafkaProducerConfig.class,
        KafkaConsumerConfig.class,
        RawEncodedRTCMJsonRouter.class,
        RawEncodedJsonService.class,
        SerializationConfig.class,
        TestMetricsConfig.class,
    },
    properties = {
        "ode.kafka.topics.raw-encoded-json.rtcm=topic.Asn1DecoderTestRTCMJSON",
        "ode.kafka.topics.asn1.decoder-input=topic.Asn1DecoderRTCMInput"
    })
@EnableConfigurationProperties
@ContextConfiguration(classes = {
    UDPReceiverProperties.class, OdeKafkaProperties.class,
    RawEncodedJsonTopics.class, KafkaProperties.class, Asn1CoderTopics.class
})
@EmbeddedKafka(partitions = 1, topics = {"topic.Asn1DecoderTestRTCMJSON", "topic.Asn1DecoderRTCMInput"})
@TestPropertySource(properties = {"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"})
@DirtiesContext
public class RawEncodedRTCMJsonRouterTest {
  
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
            "us/dot/its/jpo/ode/kafka/listeners/asn1/decoder-input-rtcm.json");
    assert inputStream != null;
    var json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    kafkaTemplate.send(rawEncodedJsonTopics.getRtcm(), json);

    inputStream = classLoader
        .getResourceAsStream("us/dot/its/jpo/ode/kafka/listeners/asn1/expected-rtcm.xml");
    assert inputStream != null;
    var expectedRTCM = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

    assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

    assertEquals(expectedRTCM, actualPayload);
  }

  @KafkaListener(topics = "topic.Asn1DecoderRTCMInput")
  public void receive(String payload) {
    this.actualPayload = payload;
    latch.countDown();
  }
}