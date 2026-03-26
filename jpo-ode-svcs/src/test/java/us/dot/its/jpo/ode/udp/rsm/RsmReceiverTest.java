package us.dot.its.jpo.ode.udp.rsm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
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
import us.dot.its.jpo.ode.kafka.producer.KafkaProducerConfig;
import us.dot.its.jpo.ode.kafka.topics.RawEncodedJsonTopics;
import us.dot.its.jpo.ode.test.utilities.TestUDPClient;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties;
import us.dot.its.jpo.ode.util.DateTimeUtils;

/**
 * Unit test for the RsmReceiver class, verifying UDP reception, Kafka publishing and output.
 */
@SpringBootTest(
    classes = {KafkaProducerConfig.class, KafkaConsumerConfig.class,
        SerializationConfig.class, TestMetricsConfig.class, },
    properties = {"ode.receivers.rsm.receiver-port=12759",
         "ode.kafka.topics.raw-encoded-json.rsm=topic.RsmReceiverTest"
    }
)
@EnableConfigurationProperties
@ContextConfiguration(classes = {UDPReceiverProperties.class, OdeKafkaProperties.class,
        RawEncodedJsonTopics.class,
        KafkaProperties.class,
})
@EmbeddedKafka(partitions = 1, topics = {"topic.RsmReceiverTest"})
@TestPropertySource(properties = {"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"})
@DirtiesContext
public class RsmReceiverTest {

  @Autowired
  UDPReceiverProperties udpReceiverProperties;

  @Autowired
  RawEncodedJsonTopics rawEncodedJsonTopics;

  @Autowired
  KafkaTemplate<String, String> kafkaTemplate;

  private CountDownLatch latch;
  private String actualPayload;

  @Test
  void testRun() throws Exception {
    latch = new CountDownLatch(1);
    actualPayload = null;

    final Clock prevClock = DateTimeUtils.setClock(
            Clock.fixed(Instant.parse("2024-11-26T23:53:21.120Z"), ZoneId.of("UTC")));

    RsmReceiver rsmReceiver = new RsmReceiver(udpReceiverProperties.getRsm(), kafkaTemplate,
            rawEncodedJsonTopics.getRsm());
    ExecutorService executorService = Executors.newCachedThreadPool();
    executorService.submit(rsmReceiver);

    String fileContent = Files.readString(Path.of("src/test/resources/us/dot/its/jpo/ode/udp/rsm/RsmReceiverTest_ValidRSM.txt"));
    String expected = Files.readString(Path.of("src/test/resources/us/dot/its/jpo/ode/udp/rsm/RsmReceiverTest_ValidRSM_expected.json"));

    TestUDPClient udpClient = new TestUDPClient(udpReceiverProperties.getRsm().getReceiverPort());
    udpClient.send(fileContent);

    assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

    assertNotEquals(expected, actualPayload);
    JSONObject producedJson = new JSONObject(actualPayload);
    JSONObject expectedJson = new JSONObject(expected);

    assertNotEquals(expectedJson.getJSONObject("metadata").get("serialId"),
            producedJson.getJSONObject("metadata").get("serialId"));
    expectedJson.getJSONObject("metadata").remove("serialId");
    producedJson.getJSONObject("metadata").remove("serialId");

    assertEquals(expectedJson.toString(2), producedJson.toString(2));

    DateTimeUtils.setClock(prevClock);
  }

  @KafkaListener(topics = "topic.RsmReceiverTest")
  public void receive(String payload) {
    this.actualPayload = payload;
    latch.countDown();
  }
}