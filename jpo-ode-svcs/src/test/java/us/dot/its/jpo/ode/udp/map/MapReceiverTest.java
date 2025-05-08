package us.dot.its.jpo.ode.udp.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static us.dot.its.jpo.ode.test.utilities.ApprovalTestCase.deserializeTestCases;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
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
import us.dot.its.jpo.ode.test.utilities.ApprovalTestCase;
import us.dot.its.jpo.ode.test.utilities.EmbeddedKafkaHolder;
import us.dot.its.jpo.ode.test.utilities.TestUDPClient;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties;
import us.dot.its.jpo.ode.util.DateTimeUtils;

@Slf4j
@SpringBootTest(
    classes = {
        OdeKafkaProperties.class,
        UDPReceiverProperties.class,
        KafkaProducerConfig.class,
        SerializationConfig.class,
        TestMetricsConfig.class,
    },
    properties = {"ode.kafka.topics.raw-encoded-json.map=topic.MapReceiverTestMAPJSON",
        "ode.receivers.map.receiver-port=12412"}
)
@EnableConfigurationProperties
@ContextConfiguration(classes = {
    UDPReceiverProperties.class,
    RawEncodedJsonTopics.class, KafkaProperties.class
})
@DirtiesContext
class MapReceiverTest {

  private final EmbeddedKafkaBroker embeddedKafka = EmbeddedKafkaHolder.getEmbeddedKafka();

  @Autowired
  UDPReceiverProperties udpReceiverProperties;

  @Autowired
  KafkaTemplate<String, String> kafkaTemplate;

  @Autowired
  RawEncodedJsonTopics rawEncodedJsonTopics;

  @Test
  void testMapReceiver() throws IOException {

    // Set the clock to a fixed time so that the MapReceiver will produce the same output every time
    final Clock prevClock = DateTimeUtils.setClock(
        Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), Clock.systemUTC().getZone()));

    MapReceiver mapReceiver = new MapReceiver(udpReceiverProperties.getMap(), kafkaTemplate,
        rawEncodedJsonTopics.getMap());
    ExecutorService executorService = Executors.newCachedThreadPool();
    executorService.submit(mapReceiver);

    EmbeddedKafkaHolder.addTopics(rawEncodedJsonTopics.getMap());

    // Set up a Kafka consumer
    Map<String, Object> consumerProps =
        KafkaTestUtils.consumerProps("test-group", "false", embeddedKafka);
    DefaultKafkaConsumerFactory<Integer, String> cf =
        new DefaultKafkaConsumerFactory<>(consumerProps);
    Consumer<Integer, String> consumer = cf.createConsumer();
    embeddedKafka.consumeFromAnEmbeddedTopic(consumer, rawEncodedJsonTopics.getMap());

    TestUDPClient udpClient = new TestUDPClient(udpReceiverProperties.getMap().getReceiverPort());

    String path =
        "src/test/resources/us.dot.its.jpo.ode.udp.map/UDPMAP_To_EncodedJSON_Validation.json";
    List<ApprovalTestCase> approvalTestCases = deserializeTestCases(path);

    for (ApprovalTestCase approvalTestCase : approvalTestCases) {
      udpClient.send(approvalTestCase.getInput());

      ConsumerRecord<Integer, String> produced =
          KafkaTestUtils.getSingleRecord(consumer, rawEncodedJsonTopics.getMap());

      JSONObject producedJson = new JSONObject(produced.value());
      JSONObject expectedJson = new JSONObject(approvalTestCase.getExpected());

      // assert that the UUIDs are different, then remove them so that the rest of the JSON can be compared
      assertNotEquals(expectedJson.getJSONObject("metadata").get("serialId"),
          producedJson.getJSONObject("metadata").get("serialId"));
      expectedJson.getJSONObject("metadata").remove("serialId");
      producedJson.getJSONObject("metadata").remove("serialId");

      assertEquals(expectedJson.toString(), producedJson.toString(),
          approvalTestCase.getDescription());
    }

    DateTimeUtils.setClock(prevClock);
  }
}

