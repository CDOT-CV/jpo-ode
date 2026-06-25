package us.dot.its.jpo.ode.udp.bsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.kafka.clients.consumer.Consumer;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
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

@EnableConfigurationProperties
@SpringBootTest(
    classes = {
        KafkaConsumerConfig.class,
        KafkaProducerConfig.class,
        SerializationConfig.class,
        TestMetricsConfig.class,
        UDPReceiverProperties.class,
        OdeKafkaProperties.class,
        RawEncodedJsonTopics.class,
        KafkaProperties.class
    },
    properties = {
        "ode.receivers.bsm.receiver-port=15458",
        "ode.kafka.topics.raw-encoded-json.bsm=topic.BsmReceiverTest"
    }
)
@EmbeddedKafka(topics = "topic.BsmReceiverTest")
@TestPropertySource(properties = {"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"})
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BsmReceiverTest {

  private static final String BASE =
      "src/test/resources/us/dot/its/jpo/ode/udp/bsm/";

  @Autowired
  UDPReceiverProperties udpReceiverProperties;

  @Autowired
  RawEncodedJsonTopics rawEncodedJsonTopics;

  @Autowired
  KafkaTemplate<String, String> kafkaTemplate;

  @Autowired
  EmbeddedKafkaBroker embeddedKafka;

  private BsmReceiver bsmReceiver;
  private ExecutorService executorService;
  private Consumer<Integer, String> consumer;
  private Clock prevClock;

  @Test
    void testRawJ2735() throws Exception {
        runTest(BASE + "BsmReceiverTest_ValidBSM.txt",
                BASE + "BsmReceiverTest_ValidBSM_expected.json");
    }

    @Test
    void testWithSignature() throws Exception {
        runTest(BASE + "BsmReceiverTest_ValidBSM_WithSignature.txt",
                BASE + "BsmReceiverTest_ValidBSM_WithSignature_expected.json");
    }

    @BeforeAll
    void startReceiver() {
        prevClock = DateTimeUtils
                .setClock(Clock.fixed(Instant.parse("2024-11-26T23:53:21.120Z"), ZoneId.of("UTC")));
        bsmReceiver = new BsmReceiver(udpReceiverProperties.getBsm(), kafkaTemplate,
                rawEncodedJsonTopics.getBsm());
        executorService = Executors.newCachedThreadPool();
        executorService.submit(bsmReceiver);

        var consumerProps = KafkaTestUtils.consumerProps(embeddedKafka, "BsmReceiverTest", true);
        consumer = new DefaultKafkaConsumerFactory<Integer, String>(consumerProps).createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, rawEncodedJsonTopics.getBsm());
    }

    @AfterAll
    void cleanup() {
        bsmReceiver.setStopped(true);
        executorService.shutdown();
        consumer.close();
        DateTimeUtils.setClock(prevClock);
    }

    private void runTest(String inputFile, String expectedFile) throws Exception {
        String fileContent = Files.readString(Paths.get(inputFile));
        String expected = Files.readString(Paths.get(expectedFile));

    TestUDPClient udpClient = new TestUDPClient(udpReceiverProperties.getBsm().getReceiverPort());
    udpClient.send(fileContent);

    var singleRecord = KafkaTestUtils.getSingleRecord(consumer, rawEncodedJsonTopics.getBsm());
    assertNotEquals(expected, singleRecord.value());
    JSONObject producedJson = new JSONObject(singleRecord.value());
    JSONObject expectedJson = new JSONObject(expected);

    assertNotEquals(expectedJson.getJSONObject("metadata").get("serialId"),
        producedJson.getJSONObject("metadata").get("serialId"));
    expectedJson.getJSONObject("metadata").remove("serialId");
    producedJson.getJSONObject("metadata").remove("serialId");

    assertEquals(expectedJson.toString(2), producedJson.toString(2));
  }
}