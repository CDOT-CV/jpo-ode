package us.dot.its.jpo.ode.udp.srm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
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
        "ode.receivers.srm.receiver-port=15459",
        "ode.kafka.topics.raw-encoded-json.srm=topic.SrmReceiverTest"
    }
)
@ContextConfiguration(classes = {
    UDPReceiverProperties.class,
    RawEncodedJsonTopics.class, KafkaProperties.class
})
class SrmReceiverTest {

  @Autowired
  UDPReceiverProperties udpReceiverProperties;

  @Autowired
  RawEncodedJsonTopics rawEncodedJsonTopics;

  @Autowired
  OdeKafkaProperties odeKafkaProperties;

  EmbeddedKafkaBroker embeddedKafka = EmbeddedKafkaHolder.getEmbeddedKafka();

  @Test
  void testRun() throws Exception {
    // Path to test input data
    String fileContent = Files.readString(Paths.get(
        "src/test/resources/us/dot/its/jpo/ode/udp/srm/SrmReceiverTest_ValidData.txt"));

    // Expected output
    String expected = Files.readString(Paths.get(
        "src/test/resources/us/dot/its/jpo/ode/udp/srm/SrmReceiverTest_ExpectedOutput.json"));

    try {
      embeddedKafka.addTopics(new NewTopic(rawEncodedJsonTopics.getSrm(), 1, (short) 1));
    } catch (Exception e) {
      // Ignore - ensure topic existence
    }

    DateTimeUtils.setClock(
        Clock.fixed(Instant.parse("2024-11-26T23:53:21.120Z"), ZoneId.of("UTC")));

    SrmReceiver srmReceiver = new SrmReceiver(
        udpReceiverProperties.getSrm(),
        odeKafkaProperties,
        rawEncodedJsonTopics.getSrm()
    );
    ExecutorService executorService = Executors.newCachedThreadPool();
    executorService.submit(srmReceiver);

    TestUDPClient udpClient = new TestUDPClient(udpReceiverProperties.getSrm().getReceiverPort());
    udpClient.send(fileContent);

    var consumerProps = KafkaTestUtils.consumerProps(
        "SrmReceiverTest", "true", embeddedKafka);
    var cf = new DefaultKafkaConsumerFactory<String, String>(consumerProps);
    var consumer = cf.createConsumer();
    embeddedKafka.consumeFromAnEmbeddedTopic(consumer, rawEncodedJsonTopics.getSrm());

    var singleRecord = KafkaTestUtils.getSingleRecord(consumer, rawEncodedJsonTopics.getSrm());
    String receivedValue = singleRecord.value();
    assertNotEquals(expected, receivedValue);

    JSONObject producedJson = new JSONObject(receivedValue);
    JSONObject expectedJson = new JSONObject(expected);

    assertNotEquals(expectedJson.getJSONObject("metadata").get("serialId"),
        producedJson.getJSONObject("metadata").get("serialId"));
    expectedJson.getJSONObject("metadata").remove("serialId");
    producedJson.getJSONObject("metadata").remove("serialId");

    assertEquals(expectedJson.toString(2), producedJson.toString(2));
  }
}