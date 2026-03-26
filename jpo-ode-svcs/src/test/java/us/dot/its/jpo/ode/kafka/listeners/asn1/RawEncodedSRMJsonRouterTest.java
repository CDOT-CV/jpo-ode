package us.dot.its.jpo.ode.kafka.listeners.asn1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import us.dot.its.jpo.ode.config.SerializationConfig;
import us.dot.its.jpo.ode.kafka.KafkaConsumerConfig;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.kafka.TestMetricsConfig;
import us.dot.its.jpo.ode.kafka.listeners.json.RawEncodedJsonService;
import us.dot.its.jpo.ode.kafka.listeners.json.RawEncodedSRMJsonRouter;
import us.dot.its.jpo.ode.kafka.producer.KafkaProducerConfig;
import us.dot.its.jpo.ode.kafka.topics.Asn1CoderTopics;
import us.dot.its.jpo.ode.kafka.topics.RawEncodedJsonTopics;
import us.dot.its.jpo.ode.test.utilities.EmbeddedKafkaHolder;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties;

@SpringBootTest(
    classes = {
        KafkaProducerConfig.class,
        KafkaConsumerConfig.class,
        RawEncodedSRMJsonRouter.class,
        RawEncodedJsonService.class,
        SerializationConfig.class,
        TestMetricsConfig.class,
    },
    properties = {
        "ode.kafka.topics.raw-encoded-json.srm=topic.Asn1DecoderTestSRMJSON",
        "ode.kafka.topics.asn1.decoder-input=topic.Asn1DecoderSRMInput"
    })
@EmbeddedKafka(partitions = 1, topics = {"topic.Asn1DecoderTestRSMJSON", "topic.Asn1DecoderSRMInput"})
@TestPropertySource(properties = {"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"})
@EnableConfigurationProperties
@ContextConfiguration(classes = {
    UDPReceiverProperties.class, OdeKafkaProperties.class,
    RawEncodedJsonTopics.class, KafkaProperties.class, Asn1CoderTopics.class
})
@DirtiesContext
class RawEncodedSRMJsonRouterTest {

  @Autowired
  Asn1CoderTopics asn1CoderTopics;
  @Autowired
  RawEncodedJsonTopics rawEncodedJsonTopics;
  @Autowired
  private KafkaTemplate<String, String> kafkaTemplate;

  private CountDownLatch latch;
  private String odeRsmData;

  @Test
  void testListen() throws JSONException, IOException, InterruptedException {
    latch = new CountDownLatch(1);
    odeRsmData = null;

    var classLoader = getClass().getClassLoader();
    InputStream inputStream = classLoader
        .getResourceAsStream(
            "us/dot/its/jpo/ode/kafka/listeners/asn1/decoder-input-srm.json");
    assert inputStream != null;
    var json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    kafkaTemplate.send(rawEncodedJsonTopics.getSrm(), json);

    assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

    inputStream = classLoader
        .getResourceAsStream("us/dot/its/jpo/ode/kafka/listeners/asn1/expected-srm.xml");
    assert inputStream != null;
    var expectedSrm = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

    assertEquals(expectedSrm, odeRsmData);
  }

    @KafkaListener(topics = {"topic.Asn1DecoderSRMInput", "topic.Asn1DecoderTestRSMJSON"} , groupId = "test-group")
    public void receive(String payload) {
        odeRsmData = payload;
        latch.countDown(); // Decrement the latch once the message is received
    }
}