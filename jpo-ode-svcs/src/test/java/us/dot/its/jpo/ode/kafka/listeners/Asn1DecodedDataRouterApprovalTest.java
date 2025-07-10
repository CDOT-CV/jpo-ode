package us.dot.its.jpo.ode.kafka.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import us.dot.its.jpo.ode.kafka.KafkaConsumerConfig;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.kafka.TestMetricsConfig;
import us.dot.its.jpo.ode.kafka.listeners.asn1.Asn1DecodedDataRouter;
import us.dot.its.jpo.ode.kafka.producer.KafkaProducerConfig;
import us.dot.its.jpo.ode.kafka.topics.JsonTopics;
import us.dot.its.jpo.ode.kafka.topics.RawEncodedJsonTopics;
import us.dot.its.jpo.ode.model.OdeMessageFrameData;
import us.dot.its.jpo.ode.test.utilities.ApprovalTestCase;
import us.dot.its.jpo.ode.test.utilities.EmbeddedKafkaHolder;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties;

@Slf4j
@SpringBootTest(
    classes = {
        Asn1DecodedDataRouter.class,
        KafkaProperties.class,
        KafkaProducerConfig.class,
        KafkaConsumerConfig.class,
        SerializationConfig.class,
        TestMetricsConfig.class,
    },
    properties = {
        "ode.kafka.topics.asn1.decoder-output=topic.Asn1DecoderOutputRouterApprovalTest",
        "ode.kafka.topics.json.map=topic.OdeMapJsonRouterApprovalTest"
    })
@EnableConfigurationProperties
@ContextConfiguration(classes = {
    UDPReceiverProperties.class, OdeKafkaProperties.class,
    RawEncodedJsonTopics.class, KafkaProperties.class, JsonTopics.class
})
@DirtiesContext
class Asn1DecodedDataRouterApprovalTest {

  @Value("${ode.kafka.topics.asn1.decoder-output}")
  private String decoderOutputTopic;

  @Value("${ode.kafka.topics.json.map}")
  private String jsonMapTopic;

  @Autowired
  KafkaTemplate<String, String> producer;

  EmbeddedKafkaBroker embeddedKafka = EmbeddedKafkaHolder.getEmbeddedKafka();

  @Test
  void testAsn1DecodedDataRouter_MAPDataFlow() throws IOException {
    String[] topics = {decoderOutputTopic, jsonMapTopic};
    EmbeddedKafkaHolder.addTopics(topics);

    Map<String, Object> consumerProps =
        KafkaTestUtils.consumerProps("testT", "false", embeddedKafka);
    DefaultKafkaConsumerFactory<Integer, String> cf =
        new DefaultKafkaConsumerFactory<>(consumerProps);

    Consumer<Integer, String> consumer = cf.createConsumer();
    embeddedKafka.consumeFromEmbeddedTopics(consumer, jsonMapTopic);

    @SuppressWarnings("checkstyle:linelength")
    List<ApprovalTestCase> jsonTestCases = ApprovalTestCase.deserializeTestCases(
        "src/test/resources/us.dot.its.jpo.ode.udp.map/Asn1DecoderRouter_ApprovalTestCases_MapJson.json");

    for (ApprovalTestCase testCase : jsonTestCases) {
      producer.send(decoderOutputTopic, testCase.getInput());

      String received = KafkaTestUtils.getSingleRecord(consumer, jsonMapTopic).value();
      ObjectMapper mapper = new ObjectMapper();
      OdeMessageFrameData receivedMapData = mapper.readValue(received, OdeMessageFrameData.class);
      OdeMessageFrameData expectedMapData = mapper.readValue(testCase.getExpected(), OdeMessageFrameData.class);
      assertEquals(expectedMapData.toJson(), receivedMapData.toJson(),
          "Failed test case: " + testCase.getDescription());
    }
    consumer.close();
  }
}
