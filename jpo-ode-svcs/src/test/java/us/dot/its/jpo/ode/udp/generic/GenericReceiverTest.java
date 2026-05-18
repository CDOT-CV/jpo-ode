package us.dot.its.jpo.ode.udp.generic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
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
    properties = {"ode.receivers.generic.receiver-port=15460",
        "ode.kafka.topics.raw-encoded-json.bsm=topic.GenericReceiverTestBSM",
        "ode.kafka.topics.raw-encoded-json.map=topic.GenericReceiverTestMAP",
        "ode.kafka.topics.raw-encoded-json.psm=topic.GenericReceiverTestPSM",
        "ode.kafka.topics.raw-encoded-json.spat=topic.GenericReceiverTestSPAT",
        "ode.kafka.topics.raw-encoded-json.ssm=topic.GenericReceiverTestSSM",
        "ode.kafka.topics.raw-encoded-json.tim=topic.GenericReceiverTestTIM",
        "ode.kafka.topics.raw-encoded-json.srm=topic.GenericReceiverTestSRM",
        "ode.kafka.topics.raw-encoded-json.sdsm=topic.GenericReceiverTestSDSM",
        "ode.kafka.topics.raw-encoded-json.rtcm=topic.GenericReceiverTestRTCM",
        "ode.kafka.topics.raw-encoded-json.rsm=topic.GenericReceiverTestRSM"})
@ContextConfiguration(classes = {UDPReceiverProperties.class, OdeKafkaProperties.class,
    RawEncodedJsonTopics.class, KafkaProperties.class})
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GenericReceiverTest {

  @Autowired
  UDPReceiverProperties udpReceiverProperties;

  @Autowired
  RawEncodedJsonTopics rawEncodedJsonTopics;

  @Autowired
  KafkaTemplate<String, String> kafkaTemplate;

  EmbeddedKafkaBroker embeddedKafka = EmbeddedKafkaHolder.getEmbeddedKafka();

  private GenericReceiver genericReceiver;
  private ExecutorService executorService;
  private Consumer<String, String> consumer;
  private Clock prevClock;

  @BeforeAll
  void startReceiver() {
    String[] topics = {rawEncodedJsonTopics.getBsm(), rawEncodedJsonTopics.getMap(),
        rawEncodedJsonTopics.getPsm(), rawEncodedJsonTopics.getSpat(),
        rawEncodedJsonTopics.getSsm(), rawEncodedJsonTopics.getTim(), rawEncodedJsonTopics.getSrm(),
        rawEncodedJsonTopics.getSdsm(), rawEncodedJsonTopics.getRtcm(),
        rawEncodedJsonTopics.getRsm()};
    EmbeddedKafkaHolder.addTopics(topics);

    genericReceiver = new GenericReceiver(udpReceiverProperties.getGeneric(), kafkaTemplate,
        rawEncodedJsonTopics);
    executorService = Executors.newCachedThreadPool();
    executorService.submit(genericReceiver);

    var consumerProps = KafkaTestUtils.consumerProps("GenericReceiverTest", "true", embeddedKafka);
    consumer = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(),
        new StringDeserializer()).createConsumer();
    embeddedKafka.consumeFromEmbeddedTopics(consumer, topics);

    prevClock = DateTimeUtils
        .setClock(Clock.fixed(Instant.parse("2024-11-26T23:53:21.120Z"), ZoneOffset.UTC));
  }

  @AfterAll
  void cleanup() {
    genericReceiver.setStopped(true);
    executorService.shutdown();
    consumer.close();
    DateTimeUtils.setClock(prevClock);
  }

  private record MsgFiles(String input, String expected) {}

  private static Map<String, MsgFiles> buildFiles(String variant) {
    String base = "src/test/resources/us/dot/its/jpo/ode/udp/";
    String srmInput = variant.isEmpty()
        ? base + "srm/SrmReceiverTest_ValidData.txt"
        : base + "srm/SrmReceiverTest_ValidData" + variant + ".txt";
    String srmExpected = variant.isEmpty()
        ? base + "srm/SrmReceiverTest_ExpectedOutput.json"
        : base + "srm/SrmReceiverTest_ExpectedOutput" + variant + ".json";
    return Map.of(
        "PSM",  new MsgFiles(base + "psm/PsmReceiverTest_ValidPSM" + variant + ".txt",
                             base + "psm/PsmReceiverTest_ValidPSM" + variant + "_expected.json"),
        "BSM",  new MsgFiles(base + "bsm/BsmReceiverTest_ValidBSM" + variant + ".txt",
                             base + "bsm/BsmReceiverTest_ValidBSM" + variant + "_expected.json"),
        "MAP",  new MsgFiles(base + "map/MapReceiverTest_ValidMAP" + variant + ".txt",
                             base + "map/MapReceiverTest_ValidMAP" + variant + "_expected.json"),
        "SPAT", new MsgFiles(base + "spat/SpatReceiverTest_ValidSPAT" + variant + ".txt",
                             base + "spat/SpatReceiverTest_ValidSPAT" + variant + "_expected.json"),
        "SSM",  new MsgFiles(base + "ssm/SsmReceiverTest_ValidSSM" + variant + ".txt",
                             base + "ssm/SsmReceiverTest_ValidSSM" + variant + "_expected.json"),
        "TIM",  new MsgFiles(base + "tim/TimReceiverTest_ValidTIM" + variant + ".txt",
                             base + "tim/TimReceiverTest_ValidTIM" + variant + "_expected.json"),
        "SRM",  new MsgFiles(srmInput, srmExpected),
        "SDSM", new MsgFiles(base + "sdsm/SdsmReceiverTest_ValidSDSM" + variant + ".txt",
                             base + "sdsm/SdsmReceiverTest_ValidSDSM" + variant + "_expected.json"),
        "RTCM", new MsgFiles(base + "rtcm/RtcmReceiverTest_ValidRTC" + variant + ".txt",
                             base + "rtcm/RtcmReceiverTest_ValidRTC" + variant + "_expected.json"),
        "RSM",  new MsgFiles(base + "rsm/RsmReceiverTest_ValidRSM" + variant + ".txt",
                             base + "rsm/RsmReceiverTest_ValidRSM" + variant + "_expected.json")
    );
  }

  static Stream<Arguments> testScenarios() {
    return Stream.of(
        Arguments.of("raw J2735 no headers", buildFiles("")),
        Arguments.of("with message signature / WSMP header", buildFiles("_WithSignature"))
    );
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("testScenarios")
  void testRun(String scenario, Map<String, MsgFiles> files) throws Exception {
    TestUDPClient udpClient =
        new TestUDPClient(udpReceiverProperties.getGeneric().getReceiverPort());

    sendAndAssert(udpClient, rawEncodedJsonTopics.getPsm(), files.get("PSM"),
        "Produced PSM message does not match expected");
    sendAndAssert(udpClient, rawEncodedJsonTopics.getBsm(), files.get("BSM"),
        "Produced BSM message does not match expected");
    sendAndAssert(udpClient, rawEncodedJsonTopics.getMap(), files.get("MAP"),
        "Produced MAP message does not match expected");
    sendAndAssert(udpClient, rawEncodedJsonTopics.getSpat(), files.get("SPAT"),
        "Produced SPAT message does not match expected");
    sendAndAssert(udpClient, rawEncodedJsonTopics.getSsm(), files.get("SSM"),
        "Produced SSM message does not match expected");
    sendAndAssert(udpClient, rawEncodedJsonTopics.getTim(), files.get("TIM"),
        "Produced TIM message does not match expected");
    sendAndAssert(udpClient, rawEncodedJsonTopics.getSrm(), files.get("SRM"),
        "Produced SRM message does not match expected");
    sendAndAssert(udpClient, rawEncodedJsonTopics.getSdsm(), files.get("SDSM"),
        "Produced SDSM message does not match expected");
    sendAndAssert(udpClient, rawEncodedJsonTopics.getRtcm(), files.get("RTCM"),
        "Produced RTCM message does not match expected");
    sendAndAssert(udpClient, rawEncodedJsonTopics.getRsm(), files.get("RSM"),
        "Produced RSM message does not match expected");
  }

  private void sendAndAssert(TestUDPClient udpClient, String topic, MsgFiles files,
      String failureMsg) throws Exception {
    String fileContent = Files.readString(Paths.get(files.input()));
    String expected = Files.readString(Paths.get(files.expected()));
    udpClient.send(fileContent);
    ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, topic);
    assertExpected(failureMsg, record.value(), expected);
  }

  private static void assertExpected(String failureMsg, String actual, String expected) {
    JSONObject producedJson = new JSONObject(actual);
    JSONObject expectedJson = new JSONObject(expected);

    assertNotEquals(expectedJson.getJSONObject("metadata").get("serialId"),
        producedJson.getJSONObject("metadata").get("serialId"));
    expectedJson.getJSONObject("metadata").remove("serialId");
    producedJson.getJSONObject("metadata").remove("serialId");

    assertEquals(expectedJson.toString(2), producedJson.toString(2), failureMsg);
  }
}
