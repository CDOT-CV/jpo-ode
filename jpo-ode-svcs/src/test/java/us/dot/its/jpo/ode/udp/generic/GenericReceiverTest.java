package us.dot.its.jpo.ode.udp.generic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.kafka.clients.admin.NewTopic;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import us.dot.its.jpo.ode.kafka.KafkaProducerConfig;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.kafka.topics.RawEncodedJsonTopics;
import us.dot.its.jpo.ode.testUtilities.EmbeddedKafkaHolder;
import us.dot.its.jpo.ode.testUtilities.TestUDPClient;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties;
import us.dot.its.jpo.ode.util.DateTimeUtils;

@RunWith(SpringRunner.class)
@EnableConfigurationProperties
@SpringBootTest(
    classes = {OdeKafkaProperties.class, UDPReceiverProperties.class, KafkaProducerConfig.class},
    properties = {
        "ode.receivers.generic.receiver-port=15460",
        "ode.kafka.topics.raw-encoded-json.bsm=topic.GenericReceiverTestBSM",
        "ode.kafka.topics.raw-encoded-json.map=topic.GenericReceiverTestMAP",
        "ode.kafka.topics.raw-encoded-json.psm=topic.GenericReceiverTestPSM",
        "ode.kafka.topics.raw-encoded-json.spat=topic.GenericReceiverTestSPAT",
        "ode.kafka.topics.raw-encoded-json.ssm=topic.GenericReceiverTestSSM",
        "ode.kafka.topics.raw-encoded-json.tim=topic.GenericReceiverTestTIM"
    }
)
@ContextConfiguration(classes = {
    UDPReceiverProperties.class, OdeKafkaProperties.class,
    RawEncodedJsonTopics.class, KafkaProperties.class
})
class GenericReceiverTest {

  @Autowired
  UDPReceiverProperties udpReceiverProperties;

  @Autowired
  RawEncodedJsonTopics rawEncodedJsonTopics;

  @Autowired
  OdeKafkaProperties odeKafkaProperties;

  EmbeddedKafkaBroker embeddedKafka = EmbeddedKafkaHolder.getEmbeddedKafka();

  @Test
  void testRun() throws Exception {
    // Read in the test and expected data from the bsm, map, spat, srm, ssm, and tim files
    String psmFileContent = Files.readString(Paths.get("src/test/resources/us/dot/its/jpo/ode/udp/psm/PsmReceiverTest_ValidPSM.txt"));
    String expectedPsm = Files.readString(Paths.get("src/test/resources/us/dot/its/jpo/ode/udp/psm/PsmReceiverTest_ValidPSM_expected.json"));

    String bsmFileContent = Files.readString(Paths.get("src/test/resources/us/dot/its/jpo/ode/udp/bsm/BsmReceiverTest_ValidBSM.txt"));
    String expectedBsm = Files.readString(Paths.get("src/test/resources/us/dot/its/jpo/ode/udp/bsm/BsmReceiverTest_ValidBSM_expected.json"));

    String mapFileContent = Files.readString(Paths.get("src/test/resources/us/dot/its/jpo/ode/udp/map/MapReceiverTest_ValidMAP.txt"));
    String expectedMap = Files.readString(Paths.get("src/test/resources/us/dot/its/jpo/ode/udp/map/MapReceiverTest_ValidMAP_expected.json"));

    String spatFileContent = Files.readString(Paths.get("src/test/resources/us/dot/its/jpo/ode/udp/spat/SpatReceiverTest_ValidSPAT.txt"));
    String expectedSpat = Files.readString(Paths.get("src/test/resources/us/dot/its/jpo/ode/udp/spat/SpatReceiverTest_ValidSPAT_expected.json"));

    String ssmFileContent = Files.readString(Paths.get("src/test/resources/us/dot/its/jpo/ode/udp/ssm/SsmReceiverTest_ValidSSM.txt"));
    String expectedSsm = Files.readString(Paths.get("src/test/resources/us/dot/its/jpo/ode/udp/ssm/SsmReceiverTest_ValidSSM_expected.json"));

    String timFileContent = Files.readString(Paths.get("src/test/resources/us/dot/its/jpo/ode/udp/tim/TimReceiverTest_ValidTIM.txt"));
    String expectedTim = Files.readString(Paths.get("src/test/resources/us/dot/its/jpo/ode/udp/tim/TimReceiverTest_ValidTIM_expected.json"));


    // Topics setup
    try {
      embeddedKafka.addTopics(
          new NewTopic(rawEncodedJsonTopics.getBsm(), 1, (short) 1),
          new NewTopic(rawEncodedJsonTopics.getMap(), 1, (short) 1),
          new NewTopic(rawEncodedJsonTopics.getPsm(), 1, (short) 1),
          new NewTopic(rawEncodedJsonTopics.getSpat(), 1, (short) 1),
          new NewTopic(rawEncodedJsonTopics.getSrm(), 1, (short) 1),
          new NewTopic(rawEncodedJsonTopics.getSsm(), 1, (short) 1),
          new NewTopic(rawEncodedJsonTopics.getTim(), 1, (short) 1)
      );
    } catch (Exception e) {
      // Ignore topic creation exceptions
    }

    GenericReceiver genericReceiver = new GenericReceiver(
        udpReceiverProperties.getGeneric(),
        odeKafkaProperties,
        rawEncodedJsonTopics
    );

    ExecutorService executorService = Executors.newCachedThreadPool();
    executorService.submit(genericReceiver);

    TestUDPClient udpClient = new TestUDPClient(udpReceiverProperties.getGeneric().getReceiverPort());

    var consumerProps = KafkaTestUtils.consumerProps("GenericReceiverTest", "true", embeddedKafka);
    var cf = new DefaultKafkaConsumerFactory<String, String>(consumerProps);
    var consumer = cf.createConsumer();
    embeddedKafka.consumeFromEmbeddedTopics(consumer,
        rawEncodedJsonTopics.getMap(),
        rawEncodedJsonTopics.getSsm(),
        rawEncodedJsonTopics.getPsm(),
        rawEncodedJsonTopics.getSpat(),
        rawEncodedJsonTopics.getTim(),
        rawEncodedJsonTopics.getBsm()
    );

    DateTimeUtils.setClock(Clock.fixed(Instant.parse("2024-11-26T23:53:21.120Z"), ZoneOffset.UTC));

    // Test the PSM path
    udpClient.send(psmFileContent);
    var psmRecord = KafkaTestUtils.getSingleRecord(consumer, rawEncodedJsonTopics.getPsm());
    assertExpected("Produced PSM message does not match expected", psmRecord.value(), expectedPsm);

    udpClient.send(bsmFileContent);
    var bsmRecord = KafkaTestUtils.getSingleRecord(consumer, rawEncodedJsonTopics.getBsm());
    assertExpected("Produced BSM message does not match expected",bsmRecord.value(), expectedBsm);

    udpClient.send(mapFileContent);
    var mapRecord = KafkaTestUtils.getSingleRecord(consumer, rawEncodedJsonTopics.getMap());
    assertExpected("Produced MAP message does not match expected", mapRecord.value(), expectedMap);

    udpClient.send(spatFileContent);
    var spatRecord = KafkaTestUtils.getSingleRecord(consumer, rawEncodedJsonTopics.getSpat());
    assertExpected("Produced SPAT message does not match expected", spatRecord.value(), expectedSpat);

    udpClient.send(ssmFileContent);
    var ssmRecord = KafkaTestUtils.getSingleRecord(consumer, rawEncodedJsonTopics.getSsm());
    assertExpected("Produced SSM message does not match expected", ssmRecord.value(), expectedSsm);

    udpClient.send(timFileContent);
    var timRecord = KafkaTestUtils.getSingleRecord(consumer, rawEncodedJsonTopics.getTim());
    assertExpected("Produced TIM message does not match expected", timRecord.value(), expectedTim);
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