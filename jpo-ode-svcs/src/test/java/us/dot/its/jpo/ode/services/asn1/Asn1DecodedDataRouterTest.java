package us.dot.its.jpo.ode.services.asn1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import us.dot.its.jpo.ode.kafka.KafkaConsumerConfig;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.kafka.producer.KafkaProducerConfig;
import us.dot.its.jpo.ode.kafka.topics.Asn1CoderTopics;
import us.dot.its.jpo.ode.kafka.topics.PojoTopics;
import us.dot.its.jpo.ode.kafka.topics.RawEncodedJsonTopics;
import us.dot.its.jpo.ode.services.asn1.message.AsnCodecMessageServiceController;
import us.dot.its.jpo.ode.test.utilities.EmbeddedKafkaHolder;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties;

@Slf4j
@SpringBootTest(
    classes = {
        AsnCodecMessageServiceController.class,
        KafkaProperties.class,
        PojoTopics.class,
        KafkaProducerConfig.class,
        KafkaConsumerConfig.class,
        RawEncodedJsonTopics.class,
        Asn1CoderTopics.class,
    },
    properties = {
        "ode.kafka.topics.pojo.bsm-during-event=topic.BsmDuringEventAsn1DecodedDataRouterTest",
        "ode.kafka.topics.pojo.rx-bsm=topic.RxBsmAsn1DecodedDataRouterTest",
        "ode.kafka.topics.pojo.tx-bsm=topic.TxBsmAsn1DecodedDataRouterTest",
        "ode.kafka.topics.pojo.bsm=topic.BsmAsn1DecodedDataRouterTest",
    }
)
@EnableConfigurationProperties
@ContextConfiguration(classes = {
    UDPReceiverProperties.class, OdeKafkaProperties.class,
    PojoTopics.class, KafkaProperties.class
})
@DirtiesContext
class Asn1DecodedDataRouterTest {

  EmbeddedKafkaBroker embeddedKafka = EmbeddedKafkaHolder.getEmbeddedKafka();
  @Autowired
  KafkaTemplate<String, String> kafkaStringTemplate;
  @Autowired
  PojoTopics pojoTopics;
  @Qualifier("kafkaListenerContainerFactory")
  @Autowired
  ConcurrentKafkaListenerContainerFactory<String, String> listenerContainerFactory;

  @Test
  void testAsn1DecodedDataRouterBSMDataFlow() {
    try {
      embeddedKafka.addTopics(
          new NewTopic(pojoTopics.getBsm(), 1, (short) 1),
          new NewTopic(pojoTopics.getBsmDuringEvent(), 1, (short) 1),
          new NewTopic(pojoTopics.getRxBsm(), 1, (short) 1),
          new NewTopic(pojoTopics.getTxBsm(), 1, (short) 1)
      );
    } catch (Exception e) {
      // Ignore because we only care they are created not that they weren't created prior
    }

    String baseTestData =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/decoder-output-bsm.xml");
    String baseExpectedSpecificBsm =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/expected-bsm-specific.xml");
    String baseExpectedBsm = loadFromResource("us/dot/its/jpo/ode/services/asn1/expected-bsm.xml");

    var testConsumer =
        listenerContainerFactory.getConsumerFactory().createConsumer();
    embeddedKafka.consumeFromAllEmbeddedTopics(testConsumer);

    String recordTypeToReplace = "bsmLogDuringEvent";
    for (String recordType : new String[] {"bsmLogDuringEvent", "rxMsg", "bsmTx"}) {
      String topic;
      switch (recordType) {
        case "bsmLogDuringEvent" -> topic = pojoTopics.getBsmDuringEvent();
        case "rxMsg" -> topic = pojoTopics.getRxBsm();
        case "bsmTx" -> topic = pojoTopics.getTxBsm();
        default -> throw new IllegalStateException("Unexpected value: " + recordType);
      }

      String inputData = replaceRecordType(baseTestData, "bsmTx", recordType);
      kafkaStringTemplate.send(topic, inputData);
      kafkaStringTemplate.send(pojoTopics.getBsm(), inputData);

      var consumedSpecific = KafkaTestUtils.getSingleRecord(testConsumer, topic);
      var consumedBsm = KafkaTestUtils.getSingleRecord(testConsumer, pojoTopics.getBsm());

      String expectedBsm = replaceRecordType(baseExpectedBsm,
          recordTypeToReplace, recordType);
      assertEquals(expectedBsm, consumedBsm.value());

      String expectedSpecificBsm = replaceRecordType(baseExpectedSpecificBsm,
          recordTypeToReplace, recordType);
      assertEquals(expectedSpecificBsm, consumedSpecific.value());
    }
  }

  @Test
  void testAsn1DecodedDataRouter_TIMDataFlow() {
    fail("Not yet implemented");
  }

  @Test
  void testAsn1DecodedDataRouter_SPaTDataFlow() {
    fail("Not yet implemented");
  }

  @Test
  void testAsn1DecodedDataRouter_SSMDataFlow() {
    fail("Not yet implemented");
  }

  @Test
  void testAsn1DecodedDataRouter_SRMDataFlow() {
    fail("Not yet implemented");
  }

  @Test
  void testAsn1DecodedDataRouter_PSMDataFlow() {
    fail("Not yet implemented");
  }

  @Test
  void testAsn1DecodedDataRouter_MAPDataFlow() {
    fail("Not yet implemented");
  }

  private String loadFromResource(String resourcePath) {
    String baseTestData;
    try (InputStream inputStream = getClass().getClassLoader()
        .getResourceAsStream(resourcePath)) {
      if (inputStream == null) {
        throw new FileNotFoundException("Resource not found: decoder-output-bsm.xml");
      }
      baseTestData = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load test data from decoder-output-bsm.xml", e);
    }
    return baseTestData;
  }

  private String replaceRecordType(String testData, String curRecordType, String recordType) {
    return testData.replace("<recordType>" + curRecordType + "</recordType>",
        "<recordType>" + recordType + "</recordType>");
  }
}
