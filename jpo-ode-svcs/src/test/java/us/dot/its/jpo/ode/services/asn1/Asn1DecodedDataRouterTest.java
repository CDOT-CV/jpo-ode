package us.dot.its.jpo.ode.services.asn1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.kafka.producer.KafkaProducerConfig;
import us.dot.its.jpo.ode.kafka.topics.Asn1CoderTopics;
import us.dot.its.jpo.ode.kafka.topics.JsonTopics;
import us.dot.its.jpo.ode.kafka.topics.PojoTopics;
import us.dot.its.jpo.ode.kafka.topics.RawEncodedJsonTopics;
import us.dot.its.jpo.ode.model.OdeBsmData;
import us.dot.its.jpo.ode.test.utilities.EmbeddedKafkaHolder;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties;
import us.dot.its.jpo.ode.wrapper.MessageConsumer;
import us.dot.its.jpo.ode.wrapper.serdes.MessagingDeserializer;

@Slf4j
@SpringBootTest(
    classes = {
        KafkaProperties.class,
        PojoTopics.class,
        JsonTopics.class,
        Asn1CoderTopics.class,
        KafkaProducerConfig.class,
        RawEncodedJsonTopics.class,
        Asn1CoderTopics.class,
        OdeKafkaProperties.class,
    }
)
@EnableConfigurationProperties
@ContextConfiguration(classes = {
    UDPReceiverProperties.class, OdeKafkaProperties.class,
    PojoTopics.class, KafkaProperties.class
})
@DirtiesContext
class Asn1DecodedDataRouterTest {

  EmbeddedKafkaBroker embeddedKafka;
  @Autowired
  KafkaTemplate<String, String> kafkaStringTemplate;
  @Autowired
  PojoTopics pojoTopics;
  @Autowired
  JsonTopics jsonTopics;
  @Autowired
  Asn1CoderTopics asn1CoderTopics;
  @Autowired
  OdeKafkaProperties odeKafkaProperties;

  ObjectMapper mapper = new ObjectMapper();
  Asn1DecodedDataRouter decoderRouter;

  @BeforeEach
  void setup() {
    // since some tests produce/consume from the same topics, we need to create unique topic names
    // to isolate the test executions
    var uuid = UUID.randomUUID().toString().split("-")[0];

    pojoTopics.setBsmDuringEvent("topic.BsmDuringEventAsn1DecodedDataRouterTest" + uuid);
    pojoTopics.setRxBsm("topic.RxBsmAsn1DecodedDataRouterTest" + uuid);
    pojoTopics.setTxBsm("topic.TxBsmAsn1DecodedDataRouterTest" + uuid);
    pojoTopics.setBsm("topic.BsmAsn1DecodedDataRouterTest" + uuid);
    jsonTopics.setDnMessage("topic.DnMessageAsn1DecodedDataRouterTest" + uuid);
    jsonTopics.setRxTim("topic.RxTimAsn1DecodedDataRouterTest" + uuid);
    jsonTopics.setTim("topic.TimAsn1DecodedDataRouterTest" + uuid);
    asn1CoderTopics.setDecoderOutput("topic.DecoderOutputAsn1DecodedDataRouterTest" + uuid);

    embeddedKafka = EmbeddedKafkaHolder.getEmbeddedKafka();
    // add the ingress topic for all test data here so that it's available to the decoderRouter
    // and the individual tests
    EmbeddedKafkaHolder.addTopics(asn1CoderTopics.getDecoderOutput());
    decoderRouter =
        new Asn1DecodedDataRouter(odeKafkaProperties, pojoTopics, jsonTopics);

    MessageConsumer<String, String> asn1DecoderConsumer =
        MessageConsumer.defaultStringMessageConsumer(
            odeKafkaProperties.getBrokers(), this.getClass().getSimpleName() + uuid, decoderRouter);

    asn1DecoderConsumer.setName("Asn1DecoderConsumer");
    decoderRouter.start(asn1DecoderConsumer, asn1CoderTopics.getDecoderOutput());
  }

  @Test
  void testAsn1DecodedDataRouterBSMDataFlow() throws IOException {
    String[] topics = Arrays.array(
        pojoTopics.getBsm(),
        pojoTopics.getBsmDuringEvent(),
        pojoTopics.getRxBsm(),
        pojoTopics.getTxBsm()
    );
    EmbeddedKafkaHolder.addTopics(topics);

    String decodedBsmXml =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/decoder-output-bsm.xml");

    var consumerProps = KafkaTestUtils.consumerProps(
        "bsmDecoderTest", "false", embeddedKafka);
    var consumerFactory = new DefaultKafkaConsumerFactory<String, OdeBsmData>(consumerProps);
    consumerFactory.setValueDeserializer(new MessagingDeserializer<>());
    var testConsumer = consumerFactory.createConsumer();
    embeddedKafka.consumeFromEmbeddedTopics(testConsumer, topics);

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
    String[] topics = Arrays.array(
        jsonTopics.getDnMessage(),
        jsonTopics.getRxTim(),
        jsonTopics.getTim()
    );
    EmbeddedKafkaHolder.addTopics(topics);

    String baseTestData =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/decoder-output-tim.xml");

    var consumerProps = KafkaTestUtils.consumerProps(
        "timDecoderTest", "false", embeddedKafka);
    var consumerFactory = new DefaultKafkaConsumerFactory<String, String>(consumerProps);
    var testConsumer = consumerFactory.createConsumer();

    embeddedKafka.consumeFromEmbeddedTopics(testConsumer, topics);

    String baseExpectedTim =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/expected-tim.json");
    for (String recordType : new String[] {"dnMsg", "rxMsg"}) {
      String topic;
      switch (recordType) {
        case "rxMsg" -> topic = jsonTopics.getRxTim();
        case "dnMsg" -> topic = jsonTopics.getDnMessage();
        default -> throw new IllegalStateException("Unexpected value: " + recordType);
      }

      String inputData = replaceRecordType(baseTestData, "timMsg", recordType);
      kafkaStringTemplate.send(asn1CoderTopics.getDecoderOutput(), inputData);

      var consumedTim = KafkaTestUtils.getSingleRecord(testConsumer, jsonTopics.getTim());
      var expectedTim = replaceJSONRecordType(baseExpectedTim, "dnMsg", recordType);
      assertEquals(expectedTim, consumedTim.value());

      var consumedSpecific = KafkaTestUtils.getSingleRecord(testConsumer, topic);
      assertEquals(expectedTim, consumedSpecific.value());
    }
  }

  @Test
  void testAsn1DecodedDataRouter_SPaTDataFlow() {
    String[] topics = Arrays.array(
        jsonTopics.getSpat(),
        jsonTopics.getRxSpat(),
        jsonTopics.getDnMessage(),
        pojoTopics.getTxSpat()
    );
    EmbeddedKafkaHolder.addTopics(topics);

    String baseTestData =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/decoder-output-spat.xml");

    var consumerProps = KafkaTestUtils.consumerProps(
        "spatDecoderTest", "false", embeddedKafka);
    var consumerFactory = new DefaultKafkaConsumerFactory<String, String>(consumerProps);
    var testConsumer = consumerFactory.createConsumer();
    embeddedKafka.consumeFromEmbeddedTopics(testConsumer, topics);

    String baseExpectedSpat =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/expected-spat.json");
    for (String recordType : new String[] {"spatTx", "rxMsg", "dnMsg"}) {
      String topic;
      switch (recordType) {
        case "rxMsg" -> topic = jsonTopics.getRxSpat();
        case "dnMsg" -> topic = jsonTopics.getDnMessage();
        case "spatTx" -> topic = pojoTopics.getTxSpat();
        default -> throw new IllegalStateException("Unexpected value: " + recordType);
      }

      String inputData = replaceRecordType(baseTestData, "spatTx", recordType);
      kafkaStringTemplate.send(asn1CoderTopics.getDecoderOutput(), inputData);

      var consumedSpecific = KafkaTestUtils.getSingleRecord(testConsumer, topic);
      var consumedSpat = KafkaTestUtils.getSingleRecord(testConsumer, jsonTopics.getSpat());

      var expectedSpat = replaceJSONRecordType(baseExpectedSpat, "spatTx", recordType);
      assertEquals(expectedSpat, consumedSpat.value());
      assertEquals(expectedSpat, consumedSpecific.value());
    }
  }

  @Test
  void testAsn1DecodedDataRouter_SSMDataFlow() {
    String[] topics = Arrays.array(
        jsonTopics.getSsm(),
        pojoTopics.getSsm()
    );
    EmbeddedKafkaHolder.addTopics(topics);

    String baseTestData =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/decoder-output-ssm.xml");

    var consumerProps = KafkaTestUtils.consumerProps(
        "ssmDecoderTest", "false", embeddedKafka);
    var consumerFactory = new DefaultKafkaConsumerFactory<String, String>(consumerProps);
    var testConsumer = consumerFactory.createConsumer();
    embeddedKafka.consumeFromEmbeddedTopics(testConsumer, topics);

    String baseExpectedSsm =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/expected-ssm.json");
    for (String recordType : new String[] {"ssmTx", "unsupported"}) {

      String inputData = replaceRecordType(baseTestData, "ssmTx", recordType);
      kafkaStringTemplate.send(asn1CoderTopics.getDecoderOutput(), inputData);

      var expectedSsm = replaceJSONRecordType(baseExpectedSsm, "ssmTx", recordType);

      var consumedSsm = KafkaTestUtils.getSingleRecord(testConsumer, jsonTopics.getSsm());
      assertEquals(expectedSsm, consumedSsm.value());

      if (recordType.equals("ssmTx")) {
        var consumedSpecific = KafkaTestUtils.getSingleRecord(testConsumer, pojoTopics.getSsm());
        assertEquals(expectedSsm, consumedSpecific.value());
      }
    }
  }

  @Test
  void testAsn1DecodedDataRouter_SRMDataFlow() {
    String[] topics = Arrays.array(
        jsonTopics.getSrm(),
        pojoTopics.getTxSrm()
    );
    EmbeddedKafkaHolder.addTopics(topics);

    String baseTestData =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/decoder-output-srm.xml");

    var consumerProps = KafkaTestUtils.consumerProps(
        "srmDecoderTest", "false", embeddedKafka);
    var consumerFactory = new DefaultKafkaConsumerFactory<String, String>(consumerProps);
    var testConsumer = consumerFactory.createConsumer();
    embeddedKafka.consumeFromEmbeddedTopics(testConsumer, topics);

    String baseExpectedSrm =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/expected-srm.json");
    for (String recordType : new String[] {"srmTx", "unsupported"}) {

      String inputData = replaceRecordType(baseTestData, "srmTx", recordType);
      kafkaStringTemplate.send(asn1CoderTopics.getDecoderOutput(), inputData);

      var expectedSrm = replaceJSONRecordType(baseExpectedSrm, "srmTx", recordType);

      var consumedSrm = KafkaTestUtils.getSingleRecord(testConsumer, jsonTopics.getSrm());
      assertEquals(expectedSrm, consumedSrm.value());

      if (recordType.equals("srmTx")) {
        var consumedSpecific = KafkaTestUtils.getSingleRecord(testConsumer, pojoTopics.getTxSrm());
        assertEquals(expectedSrm, consumedSpecific.value());
      }
    }
  }

  @Test
  void testAsn1DecodedDataRouter_PSMDataFlow() {
    String[] topics = Arrays.array(
        jsonTopics.getPsm(),
        pojoTopics.getTxPsm()
    );
    EmbeddedKafkaHolder.addTopics(topics);

    String baseTestData =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/decoder-output-psm.xml");

    var consumerProps = KafkaTestUtils.consumerProps(
        "psmDecoderTest", "false", embeddedKafka);
    var consumerFactory = new DefaultKafkaConsumerFactory<String, String>(consumerProps);
    var testConsumer = consumerFactory.createConsumer();
    embeddedKafka.consumeFromEmbeddedTopics(testConsumer, topics);

    String baseExpectedPsm =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/expected-psm.json");
    for (String recordType : new String[] {"psmTx", "unsupported"}) {

      String inputData = replaceRecordType(baseTestData, "psmTx", recordType);
      kafkaStringTemplate.send(asn1CoderTopics.getDecoderOutput(), inputData);

      var expectedPsm = replaceJSONRecordType(baseExpectedPsm, "psmTx", recordType);

      var consumedPsm = KafkaTestUtils.getSingleRecord(testConsumer, jsonTopics.getPsm());
      assertEquals(expectedPsm, consumedPsm.value());

      if (recordType.equals("psmTx")) {
        var consumedSpecific = KafkaTestUtils.getSingleRecord(testConsumer, pojoTopics.getTxPsm());
        assertEquals(expectedPsm, consumedSpecific.value());
      }
    }
  }

  @Disabled("Enable once migrated to one Spring Kafka Listener implementation")
  @Test
  void testAsn1DecodedDataRouter_MAPDataFlow() {
    String[] topics = Arrays.array(
        jsonTopics.getMap(),
        pojoTopics.getTxMap()
    );
    EmbeddedKafkaHolder.addTopics(topics);

    String baseTestData =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/decoder-output-map.xml");

    var consumerProps = KafkaTestUtils.consumerProps(
        "mapDecoderTest", "false", embeddedKafka);
    var consumerFactory = new DefaultKafkaConsumerFactory<String, String>(consumerProps);
    var testConsumer = consumerFactory.createConsumer();
    embeddedKafka.consumeFromEmbeddedTopics(testConsumer, topics);

    String baseExpectedMap =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/expected-map.json");
    for (String recordType : new String[] {"mapTx", "unsupported"}) {

      String inputData = replaceRecordType(baseTestData, "MapTx", recordType);
      kafkaStringTemplate.send(asn1CoderTopics.getDecoderOutput(), inputData);

      var expectedMap = replaceJSONRecordType(baseExpectedMap, "mapTx", recordType);

      var consumedMap = KafkaTestUtils.getSingleRecord(testConsumer, jsonTopics.getMap());
      assertEquals(expectedMap, consumedMap.value());

      if (recordType.equals("mapTx")) {
        var consumedSpecific = KafkaTestUtils.getSingleRecord(testConsumer, pojoTopics.getTxMap());
        assertEquals(expectedMap, consumedSpecific.value());
      }
    }
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

  private String replaceJSONRecordType(String testData, String curRecordType, String recordType) {
    return testData.replace("\"recordType\":\"" + curRecordType + "\"",
        "\"recordType\":\"" + recordType + "\"");
  }
}
