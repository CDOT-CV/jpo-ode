package us.dot.its.jpo.ode.udp.tim;

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
import static org.assertj.core.api.Assertions.assertThat;

@EnableConfigurationProperties
@SpringBootTest(
    classes = {
        KafkaProducerConfig.class,
        KafkaConsumerConfig.class,
        SerializationConfig.class,
        TestMetricsConfig.class,
    },
    properties = {
        "ode.receivers.tim.receiver-port=15353",
        "ode.kafka.topics.raw-encoded-json.tim=topic.TimReceiverTest"
    }
)
@ContextConfiguration(classes = {
    UDPReceiverProperties.class, OdeKafkaProperties.class,
    RawEncodedJsonTopics.class, KafkaProperties.class
})
@EmbeddedKafka(partitions = 1, topics = {"topic.TimReceiverTest"})
@TestPropertySource(properties = {"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"})
@DirtiesContext
class TimReceiverTest {

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

    TimReceiver timReceiver = new TimReceiver(udpReceiverProperties.getTim(),
        kafkaTemplate, rawEncodedJsonTopics.getTim());
    ExecutorService executorService = Executors.newCachedThreadPool();
    executorService.submit(timReceiver);

    String fileContent =
        Files.readString(Path.of(
            "src/test/resources/us/dot/its/jpo/ode/udp/tim/TimReceiverTest_ValidTIM.txt"));
    String expected = Files.readString(Path.of(
        "src/test/resources/us/dot/its/jpo/ode/udp/tim/TimReceiverTest_ValidTIM_expected.json"));

    TestUDPClient udpClient = new TestUDPClient(udpReceiverProperties.getTim().getReceiverPort());
    udpClient.send(fileContent);

    assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();

    assertNotEquals(expected, actualPayload);
    JSONObject producedJson = new JSONObject(actualPayload);
    JSONObject expectedJson = new JSONObject(expected);

    assertNotEquals(expectedJson.getJSONObject("metadata").get("serialId"),
        producedJson.getJSONObject("metadata").get("serialId"));
    expectedJson.getJSONObject("metadata").remove("serialId");
    producedJson.getJSONObject("metadata").remove("serialId");

    assertEquals(
        expectedJson.toString(2),
        producedJson.toString(2));

    DateTimeUtils.setClock(prevClock);
  }

  @KafkaListener(topics = "topic.TimReceiverTest")
  public void receive(String payload) {
    this.actualPayload = payload;
    latch.countDown();
  }
}