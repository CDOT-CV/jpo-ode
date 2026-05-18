package us.dot.its.jpo.ode.udp.ssm;

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
    properties = {"ode.receivers.ssm.receiver-port=15358",
        "ode.kafka.topics.raw-encoded-json.ssm=topic.SsmReceiverTest"})
@ContextConfiguration(
    classes = {UDPReceiverProperties.class, RawEncodedJsonTopics.class, KafkaProperties.class})
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SsmReceiverTest {

  @Autowired
  UDPReceiverProperties udpReceiverProperties;

  @Autowired
  RawEncodedJsonTopics rawEncodedJsonTopics;

  @Autowired
  KafkaTemplate<String, String> kafkaTemplate;

  EmbeddedKafkaBroker embeddedKafka = EmbeddedKafkaHolder.getEmbeddedKafka();

  private SsmReceiver ssmReceiver;
  private ExecutorService executorService;
  private Consumer<Integer, String> consumer;
  private Clock prevClock;

  @BeforeAll
  void startReceiver() {
    EmbeddedKafkaHolder.addTopics(rawEncodedJsonTopics.getSsm());
    prevClock = DateTimeUtils
        .setClock(Clock.fixed(Instant.parse("2024-11-26T23:53:21.120Z"), ZoneId.of("UTC")));
    ssmReceiver = new SsmReceiver(udpReceiverProperties.getSsm(), kafkaTemplate,
        rawEncodedJsonTopics.getSsm());
    executorService = Executors.newCachedThreadPool();
    executorService.submit(ssmReceiver);
    var consumerProps = KafkaTestUtils.consumerProps("SsmReceiverTest", "true", embeddedKafka);
    consumer = new DefaultKafkaConsumerFactory<Integer, String>(consumerProps).createConsumer();
    embeddedKafka.consumeFromAnEmbeddedTopic(consumer, rawEncodedJsonTopics.getSsm());
  }

  @AfterAll
  void cleanup() {
    ssmReceiver.setStopped(true);
    executorService.shutdown();
    consumer.close();
    DateTimeUtils.setClock(prevClock);
  }

  static Stream<Arguments> testInputs() {
    String base = "src/test/resources/us/dot/its/jpo/ode/udp/ssm/";
    return Stream.of(
        Arguments.of("raw J2735 no headers",
            base + "SsmReceiverTest_ValidSSM.txt",
            base + "SsmReceiverTest_ValidSSM_expected.json"),
        Arguments.of("with 1609.3 WSMP header",
            base + "SsmReceiverTest_ValidSSM_WithSignature.txt",
            base + "SsmReceiverTest_ValidSSM_WithSignature_expected.json")
    );
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("testInputs")
  void testRun(String description, String inputFile, String expectedFile) throws Exception {
    String fileContent = Files.readString(Paths.get(inputFile));
    String expected = Files.readString(Paths.get(expectedFile));

    TestUDPClient udpClient = new TestUDPClient(udpReceiverProperties.getSsm().getReceiverPort());
    udpClient.send(fileContent);

    var singleRecord = KafkaTestUtils.getSingleRecord(consumer, rawEncodedJsonTopics.getSsm());
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
