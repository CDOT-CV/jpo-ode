package us.dot.its.jpo.ode.services.asn1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import us.dot.its.jpo.ode.kafka.KafkaConsumerConfig;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.kafka.producer.KafkaProducerConfig;
import us.dot.its.jpo.ode.kafka.topics.Asn1CoderTopics;
import us.dot.its.jpo.ode.kafka.topics.JsonTopics;
import us.dot.its.jpo.ode.kafka.topics.PojoTopics;
import us.dot.its.jpo.ode.kafka.topics.RawEncodedJsonTopics;
import us.dot.its.jpo.ode.model.OdeBsmData;
import us.dot.its.jpo.ode.services.asn1.message.AsnCodecMessageServiceController;
import us.dot.its.jpo.ode.test.utilities.EmbeddedKafkaHolder;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties;
import us.dot.its.jpo.ode.wrapper.MessageConsumer;
import us.dot.its.jpo.ode.wrapper.serdes.MessagingDeserializer;

@Slf4j
@SpringBootTest(
    classes = {
        AsnCodecMessageServiceController.class,
        KafkaProperties.class,
        PojoTopics.class,
        JsonTopics.class,
        Asn1CoderTopics.class,
        KafkaProducerConfig.class,
        KafkaConsumerConfig.class,
        RawEncodedJsonTopics.class,
        Asn1CoderTopics.class,
        OdeKafkaProperties.class,
    },
    properties = {
        "ode.kafka.topics.asn1.decoder-output=topic.DecoderOutputAsn1DecodedDataRouterTest",
        "ode.kafka.topics.pojo.bsm-during-event=topic.BsmDuringEventAsn1DecodedDataRouterTest",
        "ode.kafka.topics.pojo.rx-bsm=topic.RxBsmAsn1DecodedDataRouterTest",
        "ode.kafka.topics.pojo.tx-bsm=topic.TxBsmAsn1DecodedDataRouterTest",
        "ode.kafka.topics.pojo.bsm=topic.BsmAsn1DecodedDataRouterTest",
        "ode.kafka.topics.json.dn-message=topic.DnMessageAsn1DecodedDataRouterTest",
        "ode.kafka.topics.json.rx-tim=topic.RxTimAsn1DecodedDataRouterTest",
        "ode.kafka.topics.json.tim=topic.TimAsn1DecodedDataRouterTest",
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
  @Autowired
  JsonTopics jsonTopics;
  @Qualifier("kafkaListenerContainerFactory")
  ConcurrentKafkaListenerContainerFactory<String, String> listenerContainerFactory;
  @Autowired
  Asn1CoderTopics asn1CoderTopics;
  @Autowired
  OdeKafkaProperties odeKafkaProperties;

  ObjectMapper mapper = new ObjectMapper();

  @BeforeEach
  void setup() {
    Asn1DecodedDataRouter decoderRouter =
        new Asn1DecodedDataRouter(odeKafkaProperties, pojoTopics, jsonTopics);

    MessageConsumer<String, String> asn1DecoderConsumer =
        MessageConsumer.defaultStringMessageConsumer(
            odeKafkaProperties.getBrokers(), this.getClass().getSimpleName(), decoderRouter);

    asn1DecoderConsumer.setName("Asn1DecoderConsumer");
    decoderRouter.start(asn1DecoderConsumer, asn1CoderTopics.getDecoderOutput());
  }

  @Test
  void testAsn1DecodedDataRouterBSMDataFlow() throws IOException {
    EmbeddedKafkaHolder.addTopics(
        pojoTopics.getBsm(),
        pojoTopics.getBsmDuringEvent(),
        pojoTopics.getRxBsm(),
        pojoTopics.getTxBsm()
    );

    String decodedBsmXml =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/decoder-output-bsm.xml");

    var consumerProps = KafkaTestUtils.consumerProps(
        "bsmDecoderTest", "false", embeddedKafka);
    var consumerFactory = new DefaultKafkaConsumerFactory<String, OdeBsmData>(consumerProps);
    consumerFactory.setValueDeserializer(new MessagingDeserializer<>());
    var testConsumer = consumerFactory.createConsumer();
    embeddedKafka.consumeFromAllEmbeddedTopics(testConsumer);

    OdeBsmData expectedBsm = mapper.readValue(
        new File("src/test/resources/us/dot/its/jpo/ode/services/asn1/expected-bsm.json"),
        OdeBsmData.class);
    for (String recordType : new String[] {"bsmLogDuringEvent", "rxMsg", "bsmTx"}) {
      String topic;
      switch (recordType) {
        case "bsmLogDuringEvent" -> topic = pojoTopics.getBsmDuringEvent();
        case "rxMsg" -> topic = pojoTopics.getRxBsm();
        case "bsmTx" -> topic = pojoTopics.getTxBsm();
        default -> throw new IllegalStateException("Unexpected value: " + recordType);
      }

      String inputData = replaceRecordType(decodedBsmXml, "bsmTx", recordType);
      kafkaStringTemplate.send(asn1CoderTopics.getDecoderOutput(), inputData);

      var consumedSpecific = KafkaTestUtils.getSingleRecord(testConsumer, topic);
      var consumedBsm = KafkaTestUtils.getSingleRecord(testConsumer, pojoTopics.getBsm());

      assertEquals(expectedBsm, consumedBsm.value());
      assertEquals(expectedBsm, consumedSpecific.value());
    }
  }

  @Test
  void testAsn1DecodedDataRouterTIMDataFlow() {
    EmbeddedKafkaHolder.addTopics(
        jsonTopics.getDnMessage(),
        jsonTopics.getRxTim(),
        jsonTopics.getTim()
    );

    String baseTestData =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/decoder-output-tim.xml");
    String baseExpectedSpecificTim =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/expected-tim-specific.xml");
    String baseExpectedTim =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/expected-tim.xml");

    var testConsumer =
        listenerContainerFactory.getConsumerFactory().createConsumer();
    embeddedKafka.consumeFromAllEmbeddedTopics(testConsumer);

    String recordTypeToReplace = "dnMsg";
    for (String recordType : new String[] {"dnMsg", "rxMsg"}) {
      String topic;
      switch (recordType) {
        case "rxMsg" -> topic = jsonTopics.getRxTim();
        case "dnMsg" -> topic = jsonTopics.getDnMessage();
        default -> throw new IllegalStateException("Unexpected value: " + recordType);
      }

      String inputData = replaceRecordType(baseTestData, "timMsg", recordType);
      kafkaStringTemplate.send(topic, inputData);
      kafkaStringTemplate.send(jsonTopics.getTim(), inputData);

      var consumedSpecific = KafkaTestUtils.getSingleRecord(testConsumer, topic);
      var consumedTim = KafkaTestUtils.getSingleRecord(testConsumer, jsonTopics.getTim());

      String expectedTim = replaceRecordType(baseExpectedTim,
          recordTypeToReplace, recordType);
      assertEquals(expectedTim, consumedTim.value());

      String expectedSpecificTim = replaceRecordType(baseExpectedSpecificTim,
          recordTypeToReplace, recordType);
      assertEquals(expectedSpecificTim, consumedSpecific.value());
    }
  }

  @Test
  void testAsn1DecodedDataRouter_SPaTDataFlow() {
    EmbeddedKafkaHolder.addTopics(
        jsonTopics.getSpat(),
        jsonTopics.getRxSpat(),
        jsonTopics.getDnMessage(),
        pojoTopics.getTxSpat()
    );

    String baseTestData =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/decoder-output-spat.xml");
    String baseExpectedSpecificSpat =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/expected-spat-specific.xml");
    String baseExpectedSpat =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/expected-spat.xml");

    var testConsumer =
        listenerContainerFactory.getConsumerFactory().createConsumer();
    embeddedKafka.consumeFromAllEmbeddedTopics(testConsumer);

    String recordTypeToReplace = "spatTx";
    for (String recordType : new String[] {"spatTx", "rxMsg", "dnMsg"}) {
      String topic;
      switch (recordType) {
        case "rxMsg" -> topic = jsonTopics.getRxSpat();
        case "dnMsg" -> topic = jsonTopics.getDnMessage();
        case "spatTx" -> topic = pojoTopics.getTxSpat();
        default -> throw new IllegalStateException("Unexpected value: " + recordType);
      }

      String inputData = replaceRecordType(baseTestData, "spatTx", recordType);
      kafkaStringTemplate.send(topic, inputData);
      kafkaStringTemplate.send(jsonTopics.getSpat(), inputData);

      var consumedSpecific = KafkaTestUtils.getSingleRecord(testConsumer, topic);
      var consumedSpat = KafkaTestUtils.getSingleRecord(testConsumer, jsonTopics.getSpat());

      String expectedSpat = replaceRecordType(baseExpectedSpat,
          recordTypeToReplace, recordType);
      assertEquals(expectedSpat, consumedSpat.value());

      String expectedSpecificSpat = replaceRecordType(baseExpectedSpecificSpat,
          recordTypeToReplace, recordType);
      assertEquals(expectedSpecificSpat, consumedSpecific.value());
    }

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
