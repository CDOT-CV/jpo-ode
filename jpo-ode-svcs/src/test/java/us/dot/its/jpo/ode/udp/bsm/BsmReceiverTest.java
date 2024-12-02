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
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
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
import us.dot.its.jpo.ode.uper.SupportedMessageType;
import us.dot.its.jpo.ode.util.DateTimeUtils;

@RunWith(SpringRunner.class)
@EnableConfigurationProperties
@SpringBootTest(
    classes = {OdeKafkaProperties.class, UDPReceiverProperties.class, KafkaProducerConfig.class},
    properties = {
        "ode.receivers.bsm.receiver-port=15352",
        "ode.kafka.topics.raw-encoded-json.bsm=topic.BsmReceiverTest"
    }
)
@ContextConfiguration(classes = {
    OdeKafkaProperties.class, UDPReceiverProperties.class,
    RawEncodedJsonTopics.class, KafkaProperties.class
})
class BsmReceiverTest {

  @Autowired
  OdeKafkaProperties odeKafkaProperties;

  @Autowired
  UDPReceiverProperties udpReceiverProperties;

  @Autowired
  RawEncodedJsonTopics rawEncodedJsonTopics;

  @Autowired
  KafkaTemplate<String, String> kafkaTemplate;

  EmbeddedKafkaBroker embeddedKafka = EmbeddedKafkaHolder.getEmbeddedKafka();

  @Test
  void testRun() throws Exception {
    String fileContent =
        Files.readString(Paths.get(
            "src/test/resources/us/dot/its/jpo/ode/udp/bsm/BsmReceiverTest_ValidBSM.json"));

    String expected = Files.readString(Paths.get("src/test/resources/us/dot/its/jpo/ode/udp/bsm/BsmReceiverTest_ValidBSM_expected.json"));
    // create the needed topic for production
    try {
      embeddedKafka.addTopics(new NewTopic(rawEncodedJsonTopics.getBsm(), 1, (short) 1));
    } catch (Exception e) {
      // ignore because we only care that the topics exist not that they're unique
    }

    DateTimeUtils.setClock(
        Clock.fixed(Instant.parse("2024-11-26T23:53:21.120Z"), ZoneId.of("UTC")));
    // create the BsmReceiver and submit it to a runner
    BsmReceiver bsmReceiver = new BsmReceiver(udpReceiverProperties.getBsm(), kafkaTemplate,
        rawEncodedJsonTopics.getBsm());
    ExecutorService executorService = Executors.newCachedThreadPool();
    executorService.submit(bsmReceiver);

    TestUDPClient udpClient = new TestUDPClient(udpReceiverProperties.getBsm().getReceiverPort());
    udpClient.send(SupportedMessageType.BSM.getStartFlag() + fileContent);

    // setup the consumer for the topic bsmreceiver produces to
    var consumerProps = KafkaTestUtils.consumerProps(
        "BsmReceiverTest", "true", embeddedKafka);
    DefaultKafkaConsumerFactory<Integer, String> cf =
        new DefaultKafkaConsumerFactory<>(consumerProps);
    Consumer<Integer, String> consumer = cf.createConsumer();
    embeddedKafka.consumeFromAnEmbeddedTopic(consumer, rawEncodedJsonTopics.getBsm());

    // read record from produce topic
    var singleRecord = KafkaTestUtils.getSingleRecord(consumer, rawEncodedJsonTopics.getBsm());
    // confirm the stream-id is different, then remove it from both so that we can test equality of all other fields
    assertNotEquals(expected, singleRecord.value());
    JSONObject producedJson = new JSONObject(singleRecord.value());
    JSONObject expectedJson = new JSONObject(expected);

    // assert that the UUIDs are different, then remove them so that the rest of the JSON can be compared
    assertNotEquals(expectedJson.getJSONObject("metadata").get("serialId"), producedJson.getJSONObject("metadata").get("serialId"));
    expectedJson.getJSONObject("metadata").remove("serialId");
    producedJson.getJSONObject("metadata").remove("serialId");

    assertEquals(
        expectedJson.toString(2),
        producedJson.toString(2));
  }
}