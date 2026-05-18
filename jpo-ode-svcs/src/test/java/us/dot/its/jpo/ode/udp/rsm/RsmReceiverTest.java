package us.dot.its.jpo.ode.udp.rsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.apache.kafka.clients.consumer.Consumer;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import us.dot.its.jpo.ode.config.SerializationConfig;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.kafka.TestMetricsConfig;
import us.dot.its.jpo.ode.kafka.producer.KafkaProducerConfig;
import us.dot.its.jpo.ode.kafka.topics.RawEncodedJsonTopics;
import us.dot.its.jpo.ode.test.utilities.EmbeddedKafkaHolder;
import us.dot.its.jpo.ode.test.utilities.TestUDPClient;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties;
import us.dot.its.jpo.ode.util.DateTimeUtils;

@EnableConfigurationProperties
@SpringBootTest(
    classes = {OdeKafkaProperties.class, UDPReceiverProperties.class, KafkaProducerConfig.class,
        SerializationConfig.class, TestMetricsConfig.class,},
    properties = {"ode.receivers.rsm.receiver-port=12759",
        "ode.kafka.topics.raw-encoded-json.rsm=topic.RsmReceiverTest"})
@ContextConfiguration(
    classes = {UDPReceiverProperties.class, RawEncodedJsonTopics.class, KafkaProperties.class})
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RsmReceiverTest {

  @Autowired
  UDPReceiverProperties udpReceiverProperties;

  @Autowired
  RawEncodedJsonTopics rawEncodedJsonTopics;

  @Autowired
  KafkaTemplate<String, String> kafkaTemplate;

  EmbeddedKafkaBroker embeddedKafka = EmbeddedKafkaHolder.getEmbeddedKafka();

  private RsmReceiver rsmReceiver;
  private ExecutorService executorService;
  private Consumer<Integer, String> consumer;
  private Clock prevClock;

  @BeforeAll
  void startReceiver() {
    EmbeddedKafkaHolder.addTopics(rawEncodedJsonTopics.getRsm());
    prevClock = DateTimeUtils
        .setClock(Clock.fixed(Instant.parse("2024-11-26T23:53:21.120Z"), ZoneId.of("UTC")));
    rsmReceiver = new RsmReceiver(udpReceiverProperties.getRsm(), kafkaTemplate,
        rawEncodedJsonTopics.getRsm());
    executorService = Executors.newCachedThreadPool();
    executorService.submit(rsmReceiver);
    var consumerProps = KafkaTestUtils.consumerProps("RsmReceiverTest", "true", embeddedKafka);
    consumer = new DefaultKafkaConsumerFactory<Integer, String>(consumerProps).createConsumer();
    embeddedKafka.consumeFromAnEmbeddedTopic(consumer, rawEncodedJsonTopics.getRsm());
  }

  @AfterAll
  void cleanup() {
    rsmReceiver.setStopped(true);
    executorService.shutdown();
    consumer.close();
    DateTimeUtils.setClock(prevClock);
  }

  static Stream<Arguments> testInputs() {
    String base = "src/test/resources/us/dot/its/jpo/ode/udp/rsm/";
    return Stream.of(
        Arguments.of("raw J2735 no headers",
            base + "RsmReceiverTest_ValidRSM.txt",
            base + "RsmReceiverTest_ValidRSM_expected.json"),
        Arguments.of("with 1609.3 WSMP header",
            base + "RsmReceiverTest_ValidRSM_WithSignature.txt",
            base + "RsmReceiverTest_ValidRSM_WithSignature_expected.json")
    );
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("testInputs")
  void testRun(String description, String inputFile, String expectedFile) throws Exception {
    String fileContent = Files.readString(Paths.get(inputFile));
    String expected = Files.readString(Paths.get(expectedFile));

    TestUDPClient udpClient = new TestUDPClient(udpReceiverProperties.getRsm().getReceiverPort());
    udpClient.send(fileContent);

    var singleRecord = KafkaTestUtils.getSingleRecord(consumer, rawEncodedJsonTopics.getRsm());
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
