package us.dot.its.jpo.ode.services.asn1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import us.dot.its.jpo.ode.kafka.KafkaConsumerConfig;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.kafka.listeners.Asn1DecodedDataListener;
import us.dot.its.jpo.ode.kafka.producer.KafkaProducerConfig;
import us.dot.its.jpo.ode.kafka.topics.JsonTopics;
import us.dot.its.jpo.ode.kafka.topics.PojoTopics;
import us.dot.its.jpo.ode.kafka.topics.RawEncodedJsonTopics;
import us.dot.its.jpo.ode.model.OdeMapData;
import us.dot.its.jpo.ode.test.utilities.ApprovalTestCase;
import us.dot.its.jpo.ode.test.utilities.EmbeddedKafkaHolder;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties;

@Slf4j
@SpringBootTest(
    classes = {
        Asn1DecodedDataListener.class,
        KafkaProperties.class,
        KafkaProducerConfig.class,
        KafkaConsumerConfig.class},
    properties = {
        "ode.kafka.topics.asn1.decoder-output=topic.Asn1DecoderOutputRouterApprovalTest",
        "ode.kafka.topics.pojo.tx-map=topic.OdeMapTxPojoRouterApprovalTest",
        "ode.kafka.topics.json.map=topic.OdeMapJsonRouterApprovalTest"
    })
@EnableConfigurationProperties
@ContextConfiguration(classes = {
    UDPReceiverProperties.class, OdeKafkaProperties.class,
    RawEncodedJsonTopics.class, KafkaProperties.class,
    PojoTopics.class, JsonTopics.class
})
@DirtiesContext
class Asn1DecodedDataRouterApprovalTest {

  @Value("${ode.kafka.topics.asn1.decoder-output}")
  private String decoderOutputTopic;

  @Value("${ode.kafka.topics.pojo.tx-map}")
  private String txMapTopic;

  @Value("${ode.kafka.topics.json.map}")
  private String jsonMapTopic;

  EmbeddedKafkaBroker embeddedKafka = EmbeddedKafkaHolder.getEmbeddedKafka();

  @Test
  void testAsn1DecodedDataRouter_MAPDataFlow() throws IOException {
    NewTopic inputTopic = new NewTopic(decoderOutputTopic, 1, (short) 1);
    NewTopic outputTopicTx = new NewTopic(txMapTopic, 1, (short) 1);
    NewTopic outputTopicJson = new NewTopic(jsonMapTopic, 1, (short) 1);
    try {
      embeddedKafka.addTopics(inputTopic, outputTopicTx, outputTopicJson);
    } catch (Exception e) {
      // Ignore. We only care that the topics exist not that we created them here
    }

    @SuppressWarnings("checkstyle:linelength")
    List<ApprovalTestCase> testCases = ApprovalTestCase.deserializeTestCases(
        "src/test/resources/us.dot.its.jpo.ode.udp.map/Asn1DecoderRouter_ApprovalTestCases_MapTxPojo.json");

    Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka);
    DefaultKafkaProducerFactory<Integer, String> producerFactory =
        new DefaultKafkaProducerFactory<>(producerProps);
    Producer<Integer, String> producer = producerFactory.createProducer();

    Map<String, Object> consumerProps =
        KafkaTestUtils.consumerProps("testT", "false", embeddedKafka);
    DefaultKafkaConsumerFactory<Integer, String> cf =
        new DefaultKafkaConsumerFactory<>(consumerProps);

    Consumer<Integer, String> consumer = cf.createConsumer();
    embeddedKafka.consumeFromEmbeddedTopics(consumer, txMapTopic, jsonMapTopic);

    for (ApprovalTestCase testCase : testCases) {
      ProducerRecord<Integer, String> producerRecord =
          new ProducerRecord<>(decoderOutputTopic, 0, 0, testCase.getInput());
      var sent = producer.send(producerRecord);
      Awaitility.await().until(sent::isDone);

      String received = KafkaTestUtils.getSingleRecord(consumer, txMapTopic).value();
      ObjectMapper mapper = new ObjectMapper();
      OdeMapData receivedMapData = mapper.readValue(received, OdeMapData.class);
      OdeMapData expectedMapData = mapper.readValue(testCase.getExpected(), OdeMapData.class);
      assertEquals(expectedMapData.toJson(), receivedMapData.toJson(),
          "Failed test case: " + testCase.getDescription());
      // discard the JSON output
      KafkaTestUtils.getSingleRecord(consumer, jsonMapTopic);
    }

    @SuppressWarnings("checkstyle:linelength")
    List<ApprovalTestCase> jsonTestCases = ApprovalTestCase.deserializeTestCases(
        "src/test/resources/us.dot.its.jpo.ode.udp.map/Asn1DecoderRouter_ApprovalTestCases_MapJson.json");

    for (ApprovalTestCase testCase : jsonTestCases) {
      ProducerRecord<Integer, String> producerRecord =
          new ProducerRecord<>(decoderOutputTopic, 0, 0, testCase.getInput());
      producer.send(producerRecord);

      String received = KafkaTestUtils.getSingleRecord(consumer, jsonMapTopic).value();
      ObjectMapper mapper = new ObjectMapper();
      OdeMapData receivedMapData = mapper.readValue(received, OdeMapData.class);
      OdeMapData expectedMapData = mapper.readValue(testCase.getExpected(), OdeMapData.class);
      assertEquals(expectedMapData.toJson(), receivedMapData.toJson(),
          "Failed test case: " + testCase.getDescription());
    }
  }

}
