package us.dot.its.jpo.ode.kafka.listeners;

import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.assertj.core.util.Arrays;
import org.awaitility.Awaitility;
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
import us.dot.its.jpo.ode.kafka.KafkaConsumerConfig;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.kafka.TestMetricsConfig;
import us.dot.its.jpo.ode.kafka.listeners.asn1.Asn1DecodedDataRouter;
import us.dot.its.jpo.ode.kafka.producer.KafkaProducerConfig;
import us.dot.its.jpo.ode.kafka.topics.Asn1CoderTopics;
import us.dot.its.jpo.ode.kafka.topics.JsonTopics;
import us.dot.its.jpo.ode.kafka.topics.PojoTopics;
import us.dot.its.jpo.ode.kafka.topics.RawEncodedJsonTopics;
import us.dot.its.jpo.ode.model.OdeLogMetadata.RecordType;
import us.dot.its.jpo.ode.model.OdeMessageFrameData;
import us.dot.its.jpo.ode.test.utilities.EmbeddedKafkaHolder;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties;
import us.dot.its.jpo.ode.util.JsonUtils;

@Slf4j
@SpringBootTest(
    classes = {KafkaProperties.class, PojoTopics.class, JsonTopics.class, Asn1CoderTopics.class,
        KafkaConsumerConfig.class, KafkaProducerConfig.class, RawEncodedJsonTopics.class,
        Asn1CoderTopics.class, OdeKafkaProperties.class, Asn1DecodedDataRouter.class,
        SerializationConfig.class, TestMetricsConfig.class},
    properties = {"ode.kafka.disabled-topics="})
@EnableConfigurationProperties
@ContextConfiguration(classes = {UDPReceiverProperties.class, OdeKafkaProperties.class,
    PojoTopics.class, KafkaProperties.class})
@DirtiesContext
class Asn1DecodedDataRouterTest {

  EmbeddedKafkaBroker embeddedKafka = EmbeddedKafkaHolder.getEmbeddedKafka();
  @Autowired
  KafkaTemplate<String, String> kafkaStringTemplate;
  @Autowired
  PojoTopics pojoTopics;
  @Autowired
  JsonTopics jsonTopics;
  @Autowired
  Asn1CoderTopics asn1CoderTopics;
  @Autowired
  private ObjectMapper simpleObjectMapper;
  @Autowired
  private XmlMapper simpleXmlMapper;

  ObjectMapper mapper = new ObjectMapper();

  @Test
  void testAsn1DecodedDataRouterBSMDataFlow() throws IOException {
    String[] topics = Arrays.array(jsonTopics.getBsm());
    EmbeddedKafkaHolder.addTopics(topics);

    String baseTestData =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/decoder-output-bsm.xml");

    var consumerProps = KafkaTestUtils.consumerProps("bsmDecoderTest", "false", embeddedKafka);
    var consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(),
        new StringDeserializer());
    var testConsumer = consumerFactory.createConsumer();
    embeddedKafka.consumeFromEmbeddedTopics(testConsumer, topics);

    String baseExpectedBsm =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/expected-bsm.json");
    for (String recordType : new String[] {"bsmTx", "rxMsg", "bsmLogDuringEvent"}) {
      String inputData = replaceRecordType(baseTestData, "bsmTx", recordType);
      var uniqueKey = UUID.randomUUID().toString();
      kafkaStringTemplate.send(asn1CoderTopics.getDecoderOutput(), uniqueKey, inputData);

      var expectedBsm = replaceJSONRecordType(baseExpectedBsm, "bsmTx", recordType);

      OdeMessageFrameData expectedBsmMFrameData =
          mapper.readValue(expectedBsm, OdeMessageFrameData.class);
      switch (recordType) {
        case "bsmTx" -> {
          expectedBsmMFrameData.getMetadata().setRecordType(RecordType.bsmTx);
        }
        case "rxMsg" -> {
          expectedBsmMFrameData.getMetadata().setRecordType(RecordType.rxMsg);
        }
        case "bsmLogDuringEvent" -> {
          expectedBsmMFrameData.getMetadata().setRecordType(RecordType.bsmLogDuringEvent);
        }
        default -> throw new IllegalStateException("Unexpected value: " + recordType);
      }

      var consumedBsm = KafkaTestUtils.getSingleRecord(testConsumer, jsonTopics.getBsm());
      OdeMessageFrameData consumedBsmMFrameData =
          mapper.readValue(consumedBsm.value(), OdeMessageFrameData.class);

      assertThat(JsonUtils.toJson(consumedBsmMFrameData, false),
          jsonEquals(JsonUtils.toJson(expectedBsmMFrameData, false)).withTolerance(0.0001));
    }
    testConsumer.close();
  }

  @Test
  void testAsn1DecodedDataRouterTIMDataFlow() throws IOException {
    String[] topics =
        Arrays.array(jsonTopics.getDnMessage(), jsonTopics.getRxTim(), jsonTopics.getTim());
    EmbeddedKafkaHolder.addTopics(topics);

    String baseTestData =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/decoder-output-tim.xml");

    var consumerProps = KafkaTestUtils.consumerProps("timDecoderTest", "false", embeddedKafka);
    var consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(),
        new StringDeserializer());
    var testConsumer = consumerFactory.createConsumer();

    embeddedKafka.consumeFromEmbeddedTopics(testConsumer, topics);

    String baseExpectedTim = loadFromResource("us/dot/its/jpo/ode/services/asn1/expected-tim.json");
    for (String recordType : new String[] {"dnMsg", "rxMsg"}) {
      String topic;
      switch (recordType) {
        case "rxMsg" -> topic = jsonTopics.getRxTim();
        case "dnMsg" -> topic = jsonTopics.getDnMessage();
        default -> throw new IllegalStateException("Unexpected value: " + recordType);
      }

      String inputData = replaceRecordType(baseTestData, "timMsg", recordType);
      var uniqueKey = UUID.randomUUID().toString();
      kafkaStringTemplate.send(asn1CoderTopics.getDecoderOutput(), uniqueKey, inputData);

      AtomicReference<ConsumerRecord<String, String>> consumedSpecific = new AtomicReference<>();
      AtomicReference<ConsumerRecord<String, String>> consumedTim = new AtomicReference<>();
      Awaitility.await().until(() -> {
        var records = KafkaTestUtils.getRecords(testConsumer);
        for (ConsumerRecord<String, String> cr : records.records(topic)) {
          if (cr.key().equals(uniqueKey)) {
            consumedSpecific.set(cr);
            break;
          }
        }
        for (ConsumerRecord<String, String> cr : records.records(jsonTopics.getTim())) {
          if (cr.key().equals(uniqueKey)) {
            consumedTim.set(cr);
            break;
          }
        }
        return consumedSpecific.get() != null && consumedTim.get() != null;
      });
      var expectedTim =
          mapper.readTree(replaceJSONRecordType(baseExpectedTim, "dnMsg", recordType));
      var actualSpecific = mapper.readTree(consumedSpecific.get().value());
      var actualTim = mapper.readTree(consumedTim.get().value());

      assertEquals(expectedTim, actualSpecific);
      assertEquals(expectedTim, actualTim);
    }
    testConsumer.close();
  }

  @Test
  void testAsn1DecodedDataRouter_SPaTDataFlow() throws IOException {
    String[] topics = Arrays.array(jsonTopics.getSpat());
    EmbeddedKafkaHolder.addTopics(topics);

    String baseTestData =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/decoder-output-spat.xml");

    var consumerProps = KafkaTestUtils.consumerProps("spatDecoderTest", "false", embeddedKafka);
    var consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(),
        new StringDeserializer());
    var testConsumer = consumerFactory.createConsumer();
    embeddedKafka.consumeFromEmbeddedTopics(testConsumer, topics);

    String baseExpectedSpat =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/expected-spat.json");
    for (String recordType : new String[] {"spatTx", "rxMsg"}) {
      String inputData = replaceRecordType(baseTestData, "spatTx", recordType);
      var uniqueKey = UUID.randomUUID().toString();
      kafkaStringTemplate.send(asn1CoderTopics.getDecoderOutput(), uniqueKey, inputData);

      var expectedSpat = replaceJSONRecordType(baseExpectedSpat, "spatTx", recordType);

      OdeMessageFrameData expectedSpatMFrameData =
          mapper.readValue(expectedSpat, OdeMessageFrameData.class);
      switch (recordType) {
        case "spatTx" -> {
          expectedSpatMFrameData.getMetadata().setRecordType(RecordType.spatTx);
        }
        case "rxMsg" -> {
          expectedSpatMFrameData.getMetadata().setRecordType(RecordType.rxMsg);
        }
        default -> throw new IllegalStateException("Unexpected value: " + recordType);
      }

      var consumedSpat = KafkaTestUtils.getSingleRecord(testConsumer, jsonTopics.getSpat());
      OdeMessageFrameData consumedSpatMFrameData =
          mapper.readValue(consumedSpat.value(), OdeMessageFrameData.class);

      assertThat(JsonUtils.toJson(consumedSpatMFrameData, false),
          jsonEquals(JsonUtils.toJson(expectedSpatMFrameData, false)).withTolerance(0.0001));
    }
    testConsumer.close();
  }

  @Test
  void testAsn1DecodedDataRouter_SSMDataFlow() throws IOException {
    String[] topics = Arrays.array(jsonTopics.getSsm());
    EmbeddedKafkaHolder.addTopics(topics);

    String baseTestData =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/decoder-output-ssm.xml");

    var consumerProps = KafkaTestUtils.consumerProps("ssmDecoderTest", "false", embeddedKafka);
    var consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(),
        new StringDeserializer());
    var testConsumer = consumerFactory.createConsumer();
    embeddedKafka.consumeFromEmbeddedTopics(testConsumer, topics);

    String baseExpectedSsm = loadFromResource("us/dot/its/jpo/ode/services/asn1/expected-ssm.json");
    for (String recordType : new String[] {"ssmTx", "unsupported"}) {
      String inputData = replaceRecordType(baseTestData, "ssmTx", recordType);
      var uniqueKey = UUID.randomUUID().toString();
      kafkaStringTemplate.send(asn1CoderTopics.getDecoderOutput(), uniqueKey, inputData);

      var expectedSsm = replaceJSONRecordType(baseExpectedSsm, "ssmTx", recordType);

      OdeMessageFrameData expectedSsmMFrameData =
          mapper.readValue(expectedSsm, OdeMessageFrameData.class);
      switch (recordType) {
        case "ssmTx" -> {
          expectedSsmMFrameData.getMetadata().setRecordType(RecordType.ssmTx);
        }
        case "unsupported" -> {
          expectedSsmMFrameData.getMetadata().setRecordType(RecordType.unsupported);
        }
        default -> throw new IllegalStateException("Unexpected value: " + recordType);
      }

      var consumedSsm = KafkaTestUtils.getSingleRecord(testConsumer, jsonTopics.getSsm());
      OdeMessageFrameData consumedSsmMFrameData =
          mapper.readValue(consumedSsm.value(), OdeMessageFrameData.class);

      assertThat(JsonUtils.toJson(consumedSsmMFrameData, false),
          jsonEquals(JsonUtils.toJson(expectedSsmMFrameData, false)).withTolerance(0.0001));
    }
    testConsumer.close();
  }

  @Test
  void testAsn1DecodedDataRouter_SRMDataFlow() throws IOException {
    String[] topics = Arrays.array(jsonTopics.getSrm());
    EmbeddedKafkaHolder.addTopics(topics);

    String baseTestData =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/decoder-output-srm.xml");

    var consumerProps = KafkaTestUtils.consumerProps("srmDecoderTest", "false", embeddedKafka);
    var consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(),
        new StringDeserializer());
    var testConsumer = consumerFactory.createConsumer();
    embeddedKafka.consumeFromEmbeddedTopics(testConsumer, topics);

    String baseExpectedSrm = loadFromResource("us/dot/its/jpo/ode/services/asn1/expected-srm.json");
    for (String recordType : new String[] {"srmTx", "unsupported"}) {
      String inputData = replaceRecordType(baseTestData, "srmTx", recordType);
      var uniqueKey = UUID.randomUUID().toString();
      kafkaStringTemplate.send(asn1CoderTopics.getDecoderOutput(), uniqueKey, inputData);

      var expectedSrm = replaceJSONRecordType(baseExpectedSrm, "srmTx", recordType);

      OdeMessageFrameData expectedSrmMFrameData =
          mapper.readValue(expectedSrm, OdeMessageFrameData.class);
      switch (recordType) {
        case "srmTx" -> {
          expectedSrmMFrameData.getMetadata().setRecordType(RecordType.srmTx);
        }
        case "unsupported" -> {
          expectedSrmMFrameData.getMetadata().setRecordType(RecordType.unsupported);
        }
        default -> throw new IllegalStateException("Unexpected value: " + recordType);
      }

      var consumedSrm = KafkaTestUtils.getSingleRecord(testConsumer, jsonTopics.getSrm());
      OdeMessageFrameData consumedSrmMFrameData =
          mapper.readValue(consumedSrm.value(), OdeMessageFrameData.class);

      assertThat(JsonUtils.toJson(consumedSrmMFrameData, false),
          jsonEquals(JsonUtils.toJson(expectedSrmMFrameData, false)).withTolerance(0.0001));
    }
    testConsumer.close();
  }

  @Test
  void testAsn1DecodedDataRouter_PSMDataFlow() throws IOException {
    String[] topics = Arrays.array(jsonTopics.getPsm());
    EmbeddedKafkaHolder.addTopics(topics);

    String baseTestData =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/decoder-output-psm.xml");

    var consumerProps = KafkaTestUtils.consumerProps("psmDecoderTest", "false", embeddedKafka);
    var consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(),
        new StringDeserializer());
    var testConsumer = consumerFactory.createConsumer();
    embeddedKafka.consumeFromEmbeddedTopics(testConsumer, topics);

    String baseExpectedPsm = loadFromResource("us/dot/its/jpo/ode/services/asn1/expected-psm.json");
    for (String recordType : new String[] {"psmTx", "unsupported"}) {
      String inputData = replaceRecordType(baseTestData, "psmTx", recordType);
      var uniqueKey = UUID.randomUUID().toString();
      kafkaStringTemplate.send(asn1CoderTopics.getDecoderOutput(), uniqueKey, inputData);

      var expectedPsm = replaceJSONRecordType(baseExpectedPsm, "psmTx", recordType);

      OdeMessageFrameData expectedPsmMFrameData =
          mapper.readValue(expectedPsm, OdeMessageFrameData.class);
      switch (recordType) {
        case "psmTx" -> {
          expectedPsmMFrameData.getMetadata().setRecordType(RecordType.psmTx);
        }
        case "unsupported" -> {
          expectedPsmMFrameData.getMetadata().setRecordType(RecordType.unsupported);
        }
        default -> throw new IllegalStateException("Unexpected value: " + recordType);
      }

      var consumedPsm = KafkaTestUtils.getSingleRecord(testConsumer, jsonTopics.getPsm());
      OdeMessageFrameData consumedPsmMFrameData =
          mapper.readValue(consumedPsm.value(), OdeMessageFrameData.class);

      assertThat(JsonUtils.toJson(consumedPsmMFrameData, false),
          jsonEquals(JsonUtils.toJson(expectedPsmMFrameData, false)).withTolerance(0.0001));
    }
    testConsumer.close();
  }

  @Test
  void testAsn1DecodedDataRouter_MAPDataFlow() throws IOException {
    String[] topics = Arrays.array(jsonTopics.getMap());
    EmbeddedKafkaHolder.addTopics(topics);

    String baseTestData =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/decoder-output-map.xml");

    var consumerProps = KafkaTestUtils.consumerProps("mapDecoderTest", "false", embeddedKafka);
    var consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(),
        new StringDeserializer());
    var testConsumer = consumerFactory.createConsumer();
    embeddedKafka.consumeFromEmbeddedTopics(testConsumer, topics);

    String baseExpectedMap = loadFromResource("us/dot/its/jpo/ode/services/asn1/expected-map.json");
    for (String recordType : new String[] {"mapTx", "unsupported"}) {
      String inputData = replaceRecordType(baseTestData, "mapTx", recordType);
      var uniqueKey = UUID.randomUUID().toString();
      kafkaStringTemplate.send(asn1CoderTopics.getDecoderOutput(), uniqueKey, inputData);

      var expectedMap = replaceJSONRecordType(baseExpectedMap, "mapTx", recordType);

      OdeMessageFrameData expectedMapMFrameData =
          mapper.readValue(expectedMap, OdeMessageFrameData.class);
      switch (recordType) {
        case "mapTx" -> {
          expectedMapMFrameData.getMetadata().setRecordType(RecordType.mapTx);
        }
        case "unsupported" -> {
          expectedMapMFrameData.getMetadata().setRecordType(RecordType.unsupported);
        }
        default -> throw new IllegalStateException("Unexpected value: " + recordType);
      }

      var consumedMap = KafkaTestUtils.getSingleRecord(testConsumer, jsonTopics.getMap());
      OdeMessageFrameData consumedMapMFrameData =
          mapper.readValue(consumedMap.value(), OdeMessageFrameData.class);

      assertThat(JsonUtils.toJson(consumedMapMFrameData, false),
          jsonEquals(JsonUtils.toJson(expectedMapMFrameData, false)).withTolerance(0.0001));
    }
    testConsumer.close();
  }

  @Test
  void testAsn1DecodedDataRouter_SDSMDataFlow() throws IOException {
    String[] topics = Arrays.array(jsonTopics.getSdsm());
    EmbeddedKafkaHolder.addTopics(topics);

    String baseTestData =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/decoder-output-sdsm.xml");

    var consumerProps = KafkaTestUtils.consumerProps("sdsmDecoderTest", "false", embeddedKafka);
    var consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(),
        new StringDeserializer());
    var testConsumer = consumerFactory.createConsumer();
    embeddedKafka.consumeFromEmbeddedTopics(testConsumer, topics);

    String baseExpectedSdsm =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/expected-sdsm.json");
    for (String recordType : new String[] {"sdsmTx", "unsupported"}) {
      String inputData = replaceRecordType(baseTestData, "sdsmTx", recordType);
      var uniqueKey = UUID.randomUUID().toString();
      kafkaStringTemplate.send(asn1CoderTopics.getDecoderOutput(), uniqueKey, inputData);

      var expectedSdsm = replaceJSONRecordType(baseExpectedSdsm, "sdsmTx", recordType);

      OdeMessageFrameData expectedSdsmMFrameData =
          mapper.readValue(expectedSdsm, OdeMessageFrameData.class);
      switch (recordType) {
        case "sdsmTx" -> {
          expectedSdsmMFrameData.getMetadata().setRecordType(RecordType.sdsmTx);
        }
        case "unsupported" -> {
          expectedSdsmMFrameData.getMetadata().setRecordType(RecordType.unsupported);
        }
        default -> throw new IllegalStateException("Unexpected value: " + recordType);
      }

      var consumedSdsm = KafkaTestUtils.getSingleRecord(testConsumer, jsonTopics.getSdsm());
      OdeMessageFrameData consumedSdsmMFrameData =
          mapper.readValue(consumedSdsm.value(), OdeMessageFrameData.class);

      assertThat(JsonUtils.toJson(consumedSdsmMFrameData, false),
          jsonEquals(JsonUtils.toJson(expectedSdsmMFrameData, false)).withTolerance(0.0001));
    }
    testConsumer.close();
  }

  @Test
  void testAsn1DecodedDataRouterException() {
    String baseTestData =
        loadFromResource("us/dot/its/jpo/ode/services/asn1/decoder-output-failed-encoding.xml");

    var uniqueKey = UUID.randomUUID().toString();
    ConsumerRecord<String, String> consumedRecord =
        new ConsumerRecord<>(asn1CoderTopics.getDecoderOutput(), 0, 0L, uniqueKey, baseTestData);

    Asn1DecodedDataRouter router =
        new Asn1DecodedDataRouter(kafkaStringTemplate, pojoTopics, jsonTopics, simpleObjectMapper, simpleXmlMapper);

    Exception exception =
        assertThrows(Asn1DecodedDataRouter.Asn1DecodedDataRouterException.class, () -> {
          router.listen(consumedRecord);
        });

    assertEquals("Error processing decoded message with code INVALID_DATA_TYPE_ERROR and message "
        + "failed ASN.1 binary decoding of element MessageFrame: more data expected. Successfully decoded 0 bytes.",
        exception.getMessage());
  }

  private String loadFromResource(String resourcePath) {
    String baseTestData;
    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
      if (inputStream == null) {
        throw new FileNotFoundException("Resource not found: " + resourcePath);
      }
      baseTestData = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load test data", e);
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
