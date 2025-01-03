/*******************************************************************************
 * Copyright 2018 572682.
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at</p>
 *
 *   <p>http://www.apache.org/licenses/LICENSE-2.0</p>
 *
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.</p>
 ******************************************************************************/

package us.dot.its.jpo.ode.traveler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;
import mockit.Capturing;
import mockit.Expectations;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.kafka.topics.Asn1CoderTopics;
import us.dot.its.jpo.ode.kafka.topics.JsonTopics;
import us.dot.its.jpo.ode.kafka.topics.PojoTopics;
import us.dot.its.jpo.ode.model.OdeMsgMetadata;
import us.dot.its.jpo.ode.model.OdeObject;
import us.dot.its.jpo.ode.model.SerialId;
import us.dot.its.jpo.ode.plugin.j2735.DdsAdvisorySituationData;
import us.dot.its.jpo.ode.plugin.j2735.builders.TravelerMessageFromHumanToAsnConverter;
import us.dot.its.jpo.ode.security.SecurityServicesProperties;
import us.dot.its.jpo.ode.test.utilities.EmbeddedKafkaHolder;
import us.dot.its.jpo.ode.util.DateTimeUtils;
import us.dot.its.jpo.ode.util.JsonUtils.JsonUtilsException;
import us.dot.its.jpo.ode.util.XmlUtils.XmlUtilsException;


@EnableConfigurationProperties
@SpringBootTest(classes = {OdeKafkaProperties.class, Asn1CoderTopics.class, PojoTopics.class,
    JsonTopics.class, TimIngestTrackerProperties.class,
    SecurityServicesProperties.class}, properties = {"ode.kafka.brokers=localhost:4242"})
@ContextConfiguration(classes = {OdeKafkaProperties.class, Asn1CoderTopics.class, PojoTopics.class,
    JsonTopics.class, TimIngestTrackerProperties.class, SecurityServicesProperties.class})
@DirtiesContext
class TimDepositControllerTest {

  @Autowired
  OdeKafkaProperties odeKafkaProperties;

  @Autowired
  Asn1CoderTopics asn1CoderTopics;

  @Autowired
  PojoTopics pojoTopics;

  @Autowired
  JsonTopics jsonTopics;

  @Autowired
  TimIngestTrackerProperties timIngestTrackerProperties;

  @Autowired
  SecurityServicesProperties securityServicesProperties;

  EmbeddedKafkaBroker embeddedKafka = EmbeddedKafkaHolder.getEmbeddedKafka();

  int consumerCount = 0;

  @Test
  void nullRequestShouldReturnEmptyError() {
    TimDepositController testTimDepositController =
        new TimDepositController(odeKafkaProperties, asn1CoderTopics, pojoTopics, jsonTopics,
            timIngestTrackerProperties, securityServicesProperties);
    ResponseEntity<String> actualResponse = testTimDepositController.postTim(null);
    Assertions.assertEquals("{\"error\":\"Empty request.\"}", actualResponse.getBody());
  }

  @Test
  void emptyRequestShouldReturnEmptyError() {
    TimDepositController testTimDepositController =
        new TimDepositController(odeKafkaProperties, asn1CoderTopics, pojoTopics, jsonTopics,
            timIngestTrackerProperties, securityServicesProperties);
    ResponseEntity<String> actualResponse = testTimDepositController.postTim("");
    Assertions.assertEquals("{\"error\":\"Empty request.\"}", actualResponse.getBody());
  }

  @Test
  void invalidJsonSyntaxShouldReturnJsonSyntaxError() {
    TimDepositController testTimDepositController =
        new TimDepositController(odeKafkaProperties, asn1CoderTopics, pojoTopics, jsonTopics,
            timIngestTrackerProperties, securityServicesProperties);
    ResponseEntity<String> actualResponse = testTimDepositController.postTim("{\"in\"va}}}on\"}}");
    Assertions.assertEquals("{\"error\":\"Malformed or non-compliant JSON syntax.\"}",
        actualResponse.getBody());
  }

  @Test
  void missingRequestElementShouldReturnMissingRequestError() {
    TimDepositController testTimDepositController =
        new TimDepositController(odeKafkaProperties, asn1CoderTopics, pojoTopics, jsonTopics,
            timIngestTrackerProperties, securityServicesProperties);
    ResponseEntity<String> actualResponse = testTimDepositController.postTim("{\"tim\":{}}");
    Assertions.assertEquals(
        "{\"error\":\"Missing or invalid argument: Request element is required as of version 3.\"}",
        actualResponse.getBody());
  }

  @Test
  void invalidTimestampShouldReturnInvalidTimestampError() {
    TimDepositController testTimDepositController =
        new TimDepositController(odeKafkaProperties, asn1CoderTopics, pojoTopics, jsonTopics,
            timIngestTrackerProperties, securityServicesProperties);
    ResponseEntity<String> actualResponse = testTimDepositController.postTim(
        "{\"request\":{},\"tim\":{\"timeStamp\":\"201-03-13T01:07:11-05:00\"}}");
    Assertions.assertEquals(
        "{\"error\":\"Invalid timestamp in tim record: 201-03-13T01:07:11-05:00\"}",
        actualResponse.getBody());
  }

  @Test
  void messageWithNoRSUsOrSDWShouldReturnWarning() {
    // prepare
    odeKafkaProperties.setDisabledTopics(Set.of());
    pojoTopics.setTimBroadcast("test.messageWithNoRSUsOrSDWShouldReturnWarning.timBroadcast.pojo");
    jsonTopics.setTimBroadcast("test.messageWithNoRSUsOrSDWShouldReturnWarning.timBroadcast.json");
    EmbeddedKafkaHolder.addTopics(pojoTopics.getTimBroadcast(), jsonTopics.getTimBroadcast());
    DateTimeUtils.setClock(
        Clock.fixed(Instant.parse("2018-03-13T01:07:11.120Z"), ZoneId.of("UTC")));
    TimDepositController testTimDepositController =
        new TimDepositController(odeKafkaProperties, asn1CoderTopics, pojoTopics, jsonTopics,
            timIngestTrackerProperties, securityServicesProperties);
    String requestBody = "{\"request\":{},\"tim\":{\"timeStamp\":\"2018-03-13T01:07:11-05:00\"}}";

    // execute
    ResponseEntity<String> actualResponse = testTimDepositController.postTim(requestBody);

    // verify
    String expectedResponseBody =
        "{\"warning\":\"Warning: TIM contains no RSU, SNMP, or SDW fields. Message only published to broadcast streams.\"}";
    Assertions.assertEquals(expectedResponseBody, actualResponse.getBody());

    var stringConsumer = createInt2StrConsumer();
    var pojoConsumer = createInt2OdeObjConsumer();
    embeddedKafka.consumeFromAnEmbeddedTopic(pojoConsumer, pojoTopics.getTimBroadcast());
    embeddedKafka.consumeFromAnEmbeddedTopic(stringConsumer, jsonTopics.getTimBroadcast());
    var singlePojoRecord =
        KafkaTestUtils.getSingleRecord(pojoConsumer, pojoTopics.getTimBroadcast());
    Assertions.assertNotNull(singlePojoRecord);
    var singleRecord = KafkaTestUtils.getSingleRecord(stringConsumer, jsonTopics.getTimBroadcast());
    Assertions.assertNotNull(
        singleRecord); // TODO: verify message contents instead of just existence

    // cleanup
    stringConsumer.close();
    pojoConsumer.close();
  }

  @Test
  void failedObjectNodeConversionShouldReturnConvertingError(@Capturing
                                                             TravelerMessageFromHumanToAsnConverter capturingTravelerMessageFromHumanToAsnConverter)
      throws JsonUtilsException,
      TravelerMessageFromHumanToAsnConverter.NoncompliantFieldsException {
    // prepare
    odeKafkaProperties.setDisabledTopics(Set.of());
    pojoTopics.setTimBroadcast(
        "test.failedObjectNodeConversionShouldReturnConvertingError.timBroadcast.pojo");
    jsonTopics.setTimBroadcast(
        "test.failedObjectNodeConversionShouldReturnConvertingError.timBroadcast.json");
    EmbeddedKafkaHolder.addTopics(pojoTopics.getTimBroadcast(), jsonTopics.getTimBroadcast());
    DateTimeUtils.setClock(
        Clock.fixed(Instant.parse("2018-03-13T01:07:11.120Z"), ZoneId.of("UTC")));
    TimDepositController testTimDepositController =
        new TimDepositController(odeKafkaProperties, asn1CoderTopics, pojoTopics, jsonTopics,
            timIngestTrackerProperties, securityServicesProperties);
    new Expectations() {

      {
        TravelerMessageFromHumanToAsnConverter.convertTravelerInputDataToEncodableTim(
            (JsonNode) any);
        result = new JsonUtilsException("testException123", null);
      }
    };
    String requestBody =
        "{\"request\":{\"rsus\":[],\"snmp\":{}},\"tim\":{\"msgCnt\":\"13\",\"timeStamp\":\"2017-03-13T01:07:11-05:00\"}}";

    // execute
    ResponseEntity<String> actualResponse = testTimDepositController.postTim(requestBody);

    // verify
    String expectedResponseBody =
        "{\"error\":\"Error converting to encodable TravelerInputData.\"}";
    Assertions.assertEquals(expectedResponseBody, actualResponse.getBody());

    var stringConsumer = createInt2StrConsumer();
    var pojoConsumer = createInt2OdeObjConsumer();
    embeddedKafka.consumeFromAnEmbeddedTopic(pojoConsumer, pojoTopics.getTimBroadcast());
    embeddedKafka.consumeFromAnEmbeddedTopic(stringConsumer, jsonTopics.getTimBroadcast());
    var singlePojoRecord =
        KafkaTestUtils.getSingleRecord(pojoConsumer, pojoTopics.getTimBroadcast());
    Assertions.assertNotNull(singlePojoRecord);
    var singleRecord = KafkaTestUtils.getSingleRecord(stringConsumer, jsonTopics.getTimBroadcast());
    Assertions.assertNotNull(
        singleRecord); // TODO: verify message contents instead of just existence

    // cleanup
    stringConsumer.close();
    pojoConsumer.close();
  }

  @Test
  void failedXmlConversionShouldReturnConversionError(
      @Capturing TimTransmogrifier capturingTimTransmogrifier)
      throws XmlUtilsException, JsonUtilsException {
    // prepare
    odeKafkaProperties.setDisabledTopics(Set.of());
    pojoTopics.setTimBroadcast(
        "test.failedXmlConversionShouldReturnConversionError.timBroadcast.pojo");
    jsonTopics.setTimBroadcast(
        "test.failedXmlConversionShouldReturnConversionError.timBroadcast.json");
    EmbeddedKafkaHolder.addTopics(pojoTopics.getTimBroadcast(), jsonTopics.getTimBroadcast());
    DateTimeUtils.setClock(
        Clock.fixed(Instant.parse("2018-03-13T01:07:11.120Z"), ZoneId.of("UTC")));
    TimDepositController testTimDepositController =
        new TimDepositController(odeKafkaProperties, asn1CoderTopics, pojoTopics, jsonTopics,
            timIngestTrackerProperties, securityServicesProperties);

    new Expectations() {
      {
        TimTransmogrifier.convertToXml((DdsAdvisorySituationData) any, (ObjectNode) any,
            (OdeMsgMetadata) any, (SerialId) any);
        result = new XmlUtilsException("testException123", null);
      }
    };
    String requestBody =
        "{\"request\":{\"rsus\":[],\"snmp\":{}},\"tim\":{\"msgCnt\":\"13\",\"timeStamp\":\"2017-03-13T01:07:11-05:00\"}}";

    // execute
    ResponseEntity<String> actualResponse = testTimDepositController.postTim(requestBody);

    // verify
    String expectedResponseBody =
        "{\"error\":\"Error sending data to ASN.1 Encoder module: testException123\"}";
    Assertions.assertEquals(expectedResponseBody, actualResponse.getBody());

    var stringConsumer = createInt2StrConsumer();
    var pojoConsumer = createInt2OdeObjConsumer();
    embeddedKafka.consumeFromAnEmbeddedTopic(pojoConsumer, pojoTopics.getTimBroadcast());
    embeddedKafka.consumeFromAnEmbeddedTopic(stringConsumer, jsonTopics.getTimBroadcast());
    var singlePojoRecord =
        KafkaTestUtils.getSingleRecord(pojoConsumer, pojoTopics.getTimBroadcast());
    Assertions.assertNotNull(singlePojoRecord);
    var singleRecord = KafkaTestUtils.getSingleRecord(stringConsumer, jsonTopics.getTimBroadcast());
    Assertions.assertNotNull(
        singleRecord); // TODO: verify message contents instead of just existence

    // cleanup
    stringConsumer.close();
    pojoConsumer.close();
  }

  @Test
  void testSuccessfulMessageReturnsSuccessMessagePost() {
    // prepare
    odeKafkaProperties.setDisabledTopics(Set.of());
    pojoTopics.setTimBroadcast("test.successfulMessageReturnsSuccessMessagePost.timBroadcast.pojo");
    jsonTopics.setTimBroadcast("test.successfulMessageReturnsSuccessMessagePost.timBroadcast.json");
    jsonTopics.setJ2735TimBroadcast(
        "test.successfulMessageReturnsSuccessMessagePost.j2735TimBroadcast.json");
    jsonTopics.setTim("test.successfulMessageReturnsSuccessMessagePost.tim.json");
    asn1CoderTopics.setEncoderInput("test.successfulMessageReturnsSuccessMessagePost.encoderInput");
    EmbeddedKafkaHolder.addTopics(pojoTopics.getTimBroadcast(), jsonTopics.getTimBroadcast(),
        jsonTopics.getJ2735TimBroadcast(), jsonTopics.getTim(), asn1CoderTopics.getEncoderInput());
    DateTimeUtils.setClock(
        Clock.fixed(Instant.parse("2018-03-13T01:07:11.120Z"), ZoneId.of("UTC")));
    TimDepositController testTimDepositController =
        new TimDepositController(odeKafkaProperties, asn1CoderTopics, pojoTopics, jsonTopics,
            timIngestTrackerProperties, securityServicesProperties);
    String requestBody =
        "{\"request\":{\"rsus\":[],\"snmp\":{}},\"tim\":{\"msgCnt\":\"13\",\"timeStamp\":\"2017-03-13T01:07:11-05:00\"}}";

    // execute
    ResponseEntity<String> actualResponse = testTimDepositController.postTim(requestBody);

    // verify
    String expectedResponseBody = "{\"success\":\"true\"}";
    Assertions.assertEquals(expectedResponseBody, actualResponse.getBody());

    var pojoTimBroadcastConsumer = createInt2OdeObjConsumer();
    var jsonTimBroadcastConsumer = createInt2StrConsumer();
    var jsonJ2735TimBroadcastConsumer = createStr2StrConsumer();
    var jsonTimConsumer = createStr2StrConsumer();
    var asn1CoderEncoderInputConsumer = createStr2StrConsumer();

    embeddedKafka.consumeFromAnEmbeddedTopic(pojoTimBroadcastConsumer,
        pojoTopics.getTimBroadcast());
    embeddedKafka.consumeFromAnEmbeddedTopic(jsonTimBroadcastConsumer,
        jsonTopics.getTimBroadcast());
    embeddedKafka.consumeFromAnEmbeddedTopic(jsonJ2735TimBroadcastConsumer,
        jsonTopics.getJ2735TimBroadcast());
    embeddedKafka.consumeFromAnEmbeddedTopic(jsonTimConsumer, jsonTopics.getTim());
    embeddedKafka.consumeFromAnEmbeddedTopic(asn1CoderEncoderInputConsumer,
        asn1CoderTopics.getEncoderInput());

    var pojoTimBroadcastRecord =
        KafkaTestUtils.getSingleRecord(pojoTimBroadcastConsumer, pojoTopics.getTimBroadcast());
    Assertions.assertNotNull(pojoTimBroadcastRecord);
    var jsonTimBroadcastRecord =
        KafkaTestUtils.getSingleRecord(jsonTimBroadcastConsumer, jsonTopics.getTimBroadcast());
    Assertions.assertNotNull(
        jsonTimBroadcastRecord); // TODO: verify message contents instead of just existence
    var jsonJ2735TimBroadcastRecord = KafkaTestUtils.getSingleRecord(jsonJ2735TimBroadcastConsumer,
        jsonTopics.getJ2735TimBroadcast());
    Assertions.assertNotNull(
        jsonJ2735TimBroadcastRecord); // TODO: verify message contents instead of just existence
    var jsonTimRecord = KafkaTestUtils.getSingleRecord(jsonTimConsumer, jsonTopics.getTim());
    Assertions.assertNotNull(
        jsonTimRecord); // TODO: verify message contents instead of just existence
    var asn1CoderEncoderInputRecord = KafkaTestUtils.getSingleRecord(asn1CoderEncoderInputConsumer,
        asn1CoderTopics.getEncoderInput());
    Assertions.assertNotNull(
        asn1CoderEncoderInputRecord); // TODO: verify message contents instead of just existence

    // cleanup
    pojoTimBroadcastConsumer.close();
    jsonTimBroadcastConsumer.close();
    jsonJ2735TimBroadcastConsumer.close();
    jsonTimConsumer.close();
    asn1CoderEncoderInputConsumer.close();
  }

  @Test
  void testSuccessfulSdwRequestMessageReturnsSuccessMessagePost() throws Exception {
    // prepare
    odeKafkaProperties.setDisabledTopics(Set.of());
    pojoTopics.setTimBroadcast(
        "test.successfulSdwRequestMessageReturnsSuccessMessagePost.timBroadcast.pojo");
    jsonTopics.setTimBroadcast(
        "test.successfulSdwRequestMessageReturnsSuccessMessagePost.timBroadcast.json");
    jsonTopics.setJ2735TimBroadcast(
        "test.successfulSdwRequestMessageReturnsSuccessMessagePost.j2735TimBroadcast.json");
    jsonTopics.setTim("test.successfulSdwRequestMessageReturnsSuccessMessagePost.tim.json");
    asn1CoderTopics.setEncoderInput(
        "test.successfulSdwRequestMessageReturnsSuccessMessagePost.encoderInput");
    EmbeddedKafkaHolder.addTopics(pojoTopics.getTimBroadcast(), jsonTopics.getTimBroadcast(),
        jsonTopics.getJ2735TimBroadcast(), jsonTopics.getTim(), asn1CoderTopics.getEncoderInput());
    DateTimeUtils.setClock(
        Clock.fixed(Instant.parse("2018-03-13T01:07:11.120Z"), ZoneId.of("UTC")));
    TimDepositController testTimDepositController =
        new TimDepositController(odeKafkaProperties, asn1CoderTopics, pojoTopics, jsonTopics,
            timIngestTrackerProperties, securityServicesProperties);
    String file = "/sdwRequest.json";
    String requestBody =
        IOUtils.toString(TimDepositControllerTest.class.getResourceAsStream(file), "UTF-8");

    // execute
    ResponseEntity<String> actualResponse = testTimDepositController.postTim(requestBody);

    // verify
    String expectedResponseBody = "{\"success\":\"true\"}";
    Assertions.assertEquals(expectedResponseBody, actualResponse.getBody());

    var pojoTimBroadcastConsumer = createInt2OdeObjConsumer();
    var jsonTimBroadcastConsumer = createInt2StrConsumer();
    var jsonJ2735TimBroadcastConsumer = createStr2StrConsumer();
    var jsonTimConsumer = createStr2StrConsumer();
    var asn1CoderEncoderInputConsumer = createStr2StrConsumer();

    embeddedKafka.consumeFromAnEmbeddedTopic(pojoTimBroadcastConsumer,
        pojoTopics.getTimBroadcast());
    embeddedKafka.consumeFromAnEmbeddedTopic(jsonTimBroadcastConsumer,
        jsonTopics.getTimBroadcast());
    embeddedKafka.consumeFromAnEmbeddedTopic(jsonJ2735TimBroadcastConsumer,
        jsonTopics.getJ2735TimBroadcast());
    embeddedKafka.consumeFromAnEmbeddedTopic(jsonTimConsumer, jsonTopics.getTim());
    embeddedKafka.consumeFromAnEmbeddedTopic(asn1CoderEncoderInputConsumer,
        asn1CoderTopics.getEncoderInput());

    var pojoTimBroadcastRecord =
        KafkaTestUtils.getSingleRecord(pojoTimBroadcastConsumer, pojoTopics.getTimBroadcast());
    Assertions.assertNotNull(pojoTimBroadcastRecord);
    var jsonTimBroadcastRecord =
        KafkaTestUtils.getSingleRecord(jsonTimBroadcastConsumer, jsonTopics.getTimBroadcast());
    Assertions.assertNotNull(
        jsonTimBroadcastRecord); // TODO: verify message contents instead of just existence
    var jsonJ2735TimBroadcastRecord = KafkaTestUtils.getSingleRecord(jsonJ2735TimBroadcastConsumer,
        jsonTopics.getJ2735TimBroadcast());
    Assertions.assertNotNull(
        jsonJ2735TimBroadcastRecord); // TODO: verify message contents instead of just existence
    var jsonTimRecord = KafkaTestUtils.getSingleRecord(jsonTimConsumer, jsonTopics.getTim());
    Assertions.assertNotNull(
        jsonTimRecord); // TODO: verify message contents instead of just existence
    var asn1CoderEncoderInputRecord = KafkaTestUtils.getSingleRecord(asn1CoderEncoderInputConsumer,
        asn1CoderTopics.getEncoderInput());
    Assertions.assertNotNull(
        asn1CoderEncoderInputRecord); // TODO: verify message contents instead of just existence

    // cleanup
    pojoTimBroadcastConsumer.close();
    jsonTimBroadcastConsumer.close();
    jsonJ2735TimBroadcastConsumer.close();
    jsonTimConsumer.close();
    asn1CoderEncoderInputConsumer.close();
  }

  @Test
  void testSuccessfulMessageReturnsSuccessMessagePostWithOde() {
    // prepare
    odeKafkaProperties.setDisabledTopics(Set.of());
    pojoTopics.setTimBroadcast(
        "test.successfulMessageReturnsSuccessMessagePostWithOde.timBroadcast.pojo");
    jsonTopics.setTimBroadcast(
        "test.successfulMessageReturnsSuccessMessagePostWithOde.timBroadcast.json");
    jsonTopics.setJ2735TimBroadcast(
        "test.successfulMessageReturnsSuccessMessagePostWithOde.j2735TimBroadcast.json");
    jsonTopics.setTim("test.successfulMessageReturnsSuccessMessagePostWithOde.tim.json");
    asn1CoderTopics.setEncoderInput(
        "test.successfulMessageReturnsSuccessMessagePostWithOde.encoderInput");
    EmbeddedKafkaHolder.addTopics(pojoTopics.getTimBroadcast(), jsonTopics.getTimBroadcast(),
        jsonTopics.getJ2735TimBroadcast(), jsonTopics.getTim(), asn1CoderTopics.getEncoderInput());
    DateTimeUtils.setClock(
        Clock.fixed(Instant.parse("2018-03-13T01:07:11.120Z"), ZoneId.of("UTC")));
    TimDepositController testTimDepositController =
        new TimDepositController(odeKafkaProperties, asn1CoderTopics, pojoTopics, jsonTopics,
            timIngestTrackerProperties, securityServicesProperties);
    String requestBody =
        "{\"request\":{\"ode\":{},\"rsus\":[],\"snmp\":{}},\"tim\":{\"msgCnt\":\"13\",\"timeStamp\":\"2017-03-13T01:07:11-05:00\"}}";

    // execute
    ResponseEntity<String> actualResponse = testTimDepositController.postTim(requestBody);

    // verify
    String expectedResponseBody = "{\"success\":\"true\"}";
    Assertions.assertEquals(expectedResponseBody, actualResponse.getBody());

    var pojoTimBroadcastConsumer = createInt2OdeObjConsumer();
    var jsonTimBroadcastConsumer = createInt2StrConsumer();
    var jsonJ2735TimBroadcastConsumer = createStr2StrConsumer();
    var jsonTimConsumer = createStr2StrConsumer();
    var asn1CoderEncoderInputConsumer = createStr2StrConsumer();

    embeddedKafka.consumeFromAnEmbeddedTopic(pojoTimBroadcastConsumer,
        pojoTopics.getTimBroadcast());
    embeddedKafka.consumeFromAnEmbeddedTopic(jsonTimBroadcastConsumer,
        jsonTopics.getTimBroadcast());
    embeddedKafka.consumeFromAnEmbeddedTopic(jsonJ2735TimBroadcastConsumer,
        jsonTopics.getJ2735TimBroadcast());
    embeddedKafka.consumeFromAnEmbeddedTopic(jsonTimConsumer, jsonTopics.getTim());
    embeddedKafka.consumeFromAnEmbeddedTopic(asn1CoderEncoderInputConsumer,
        asn1CoderTopics.getEncoderInput());

    var pojoTimBroadcastRecord =
        KafkaTestUtils.getSingleRecord(pojoTimBroadcastConsumer, pojoTopics.getTimBroadcast());
    Assertions.assertNotNull(pojoTimBroadcastRecord);
    var jsonTimBroadcastRecord =
        KafkaTestUtils.getSingleRecord(jsonTimBroadcastConsumer, jsonTopics.getTimBroadcast());
    Assertions.assertNotNull(
        jsonTimBroadcastRecord); // TODO: verify message contents instead of just existence
    var jsonJ2735TimBroadcastRecord = KafkaTestUtils.getSingleRecord(jsonJ2735TimBroadcastConsumer,
        jsonTopics.getJ2735TimBroadcast());
    Assertions.assertNotNull(
        jsonJ2735TimBroadcastRecord); // TODO: verify message contents instead of just existence
    var jsonTimRecord = KafkaTestUtils.getSingleRecord(jsonTimConsumer, jsonTopics.getTim());
    Assertions.assertNotNull(
        jsonTimRecord); // TODO: verify message contents instead of just existence
    var asn1CoderEncoderInputRecord = KafkaTestUtils.getSingleRecord(asn1CoderEncoderInputConsumer,
        asn1CoderTopics.getEncoderInput());
    Assertions.assertNotNull(
        asn1CoderEncoderInputRecord); // TODO: verify message contents instead of just existence

    // cleanup
    pojoTimBroadcastConsumer.close();
    jsonTimBroadcastConsumer.close();
    jsonJ2735TimBroadcastConsumer.close();
    jsonTimConsumer.close();
    asn1CoderEncoderInputConsumer.close();
  }

  @Test
  void testSuccessfulMessageReturnsSuccessMessagePut() {
    // prepare
    odeKafkaProperties.setDisabledTopics(Set.of());
    pojoTopics.setTimBroadcast("test.successfulMessageReturnsSuccessMessagePut.timBroadcast.pojo");
    jsonTopics.setTimBroadcast("test.successfulMessageReturnsSuccessMessagePut.timBroadcast.json");
    jsonTopics.setJ2735TimBroadcast(
        "test.successfulMessageReturnsSuccessMessagePut.j2735TimBroadcast.json");
    jsonTopics.setTim("test.successfulMessageReturnsSuccessMessagePut.tim.json");
    asn1CoderTopics.setEncoderInput("test.successfulMessageReturnsSuccessMessagePut.encoderInput");
    EmbeddedKafkaHolder.addTopics(pojoTopics.getTimBroadcast(), jsonTopics.getTimBroadcast(),
        jsonTopics.getJ2735TimBroadcast(), jsonTopics.getTim(), asn1CoderTopics.getEncoderInput());
    DateTimeUtils.setClock(
        Clock.fixed(Instant.parse("2018-03-13T01:07:11.120Z"), ZoneId.of("UTC")));
    TimDepositController testTimDepositController =
        new TimDepositController(odeKafkaProperties, asn1CoderTopics, pojoTopics, jsonTopics,
            timIngestTrackerProperties, securityServicesProperties);
    String requestBody =
        "{\"request\":{\"rsus\":[],\"snmp\":{}},\"tim\":{\"msgCnt\":\"13\",\"timeStamp\":\"2017-03-13T01:07:11-05:00\"}}";

    // execute
    ResponseEntity<String> actualResponse = testTimDepositController.putTim(requestBody);

    // verify
    String expectedResponseBody = "{\"success\":\"true\"}";
    Assertions.assertEquals(expectedResponseBody, actualResponse.getBody());

    var pojoTimBroadcastConsumer = createInt2OdeObjConsumer();
    var jsonTimBroadcastConsumer = createInt2StrConsumer();
    var jsonJ2735TimBroadcastConsumer = createStr2StrConsumer();
    var jsonTimConsumer = createStr2StrConsumer();
    var asn1CoderEncoderInputConsumer = createStr2StrConsumer();

    embeddedKafka.consumeFromAnEmbeddedTopic(pojoTimBroadcastConsumer,
        pojoTopics.getTimBroadcast());
    embeddedKafka.consumeFromAnEmbeddedTopic(jsonTimBroadcastConsumer,
        jsonTopics.getTimBroadcast());
    embeddedKafka.consumeFromAnEmbeddedTopic(jsonJ2735TimBroadcastConsumer,
        jsonTopics.getJ2735TimBroadcast());
    embeddedKafka.consumeFromAnEmbeddedTopic(jsonTimConsumer, jsonTopics.getTim());
    embeddedKafka.consumeFromAnEmbeddedTopic(asn1CoderEncoderInputConsumer,
        asn1CoderTopics.getEncoderInput());

    var pojoTimBroadcastRecord =
        KafkaTestUtils.getSingleRecord(pojoTimBroadcastConsumer, pojoTopics.getTimBroadcast());
    Assertions.assertNotNull(pojoTimBroadcastRecord);
    var jsonTimBroadcastRecord =
        KafkaTestUtils.getSingleRecord(jsonTimBroadcastConsumer, jsonTopics.getTimBroadcast());
    Assertions.assertNotNull(
        jsonTimBroadcastRecord); // TODO: verify message contents instead of just existence
    var jsonJ2735TimBroadcastRecord = KafkaTestUtils.getSingleRecord(jsonJ2735TimBroadcastConsumer,
        jsonTopics.getJ2735TimBroadcast());
    Assertions.assertNotNull(
        jsonJ2735TimBroadcastRecord); // TODO: verify message contents instead of just existence
    var jsonTimRecord = KafkaTestUtils.getSingleRecord(jsonTimConsumer, jsonTopics.getTim());
    Assertions.assertNotNull(
        jsonTimRecord); // TODO: verify message contents instead of just existence
    var asn1CoderEncoderInputRecord = KafkaTestUtils.getSingleRecord(asn1CoderEncoderInputConsumer,
        asn1CoderTopics.getEncoderInput());
    Assertions.assertNotNull(
        asn1CoderEncoderInputRecord); // TODO: verify message contents instead of just existence

    // cleanup
    pojoTimBroadcastConsumer.close();
    jsonTimBroadcastConsumer.close();
    jsonJ2735TimBroadcastConsumer.close();
    jsonTimConsumer.close();
    asn1CoderEncoderInputConsumer.close();
  }

  @Test
  void testDepositingTimWithExtraProperties() {

    TimDepositController testTimDepositController =
        new TimDepositController(odeKafkaProperties, asn1CoderTopics, pojoTopics, jsonTopics,
            timIngestTrackerProperties, securityServicesProperties);

    String timToSubmit =
        "{\"request\":{\"rsus\":[],\"snmp\":{},\"randomProp1\":true,\"randomProp2\":\"hello world\"},\"tim\":{\"msgCnt\":\"13\",\"timeStamp\":\"2017-03-13T01:07:11-05:00\",\"randomProp3\":123,\"randomProp4\":{\"nestedProp1\":\"foo\",\"nestedProp2\":\"bar\"}}}";
    ResponseEntity<String> actualResponse = testTimDepositController.postTim(timToSubmit);
    Assertions.assertEquals("{\"success\":\"true\"}", actualResponse.getBody());

    // TODO: verify message is published to Kafka topics
  }

  @Test
  void testSuccessfulTimIngestIsTracked() {

    TimDepositController testTimDepositController =
        new TimDepositController(odeKafkaProperties, asn1CoderTopics, pojoTopics, jsonTopics,
            timIngestTrackerProperties, securityServicesProperties);

    String timToSubmit =
        "{\"request\":{\"rsus\":[],\"snmp\":{},\"randomProp1\":true,\"randomProp2\":\"hello world\"},\"tim\":{\"msgCnt\":\"13\",\"timeStamp\":\"2017-03-13T01:07:11-05:00\",\"randomProp3\":123,\"randomProp4\":{\"nestedProp1\":\"foo\",\"nestedProp2\":\"bar\"}}}";
    long priorIngestCount = TimIngestTracker.getInstance().getTotalMessagesReceived();
    ResponseEntity<String> actualResponse = testTimDepositController.postTim(timToSubmit);
    Assertions.assertEquals("{\"success\":\"true\"}", actualResponse.getBody());
    Assertions.assertEquals(priorIngestCount + 1,
        TimIngestTracker.getInstance().getTotalMessagesReceived());

    // TODO: verify message is published to Kafka topics
  }

  // This serves as an integration test without mocking the TimTransmogrifier and XmlUtils
  @Test
  void testSuccessfulRsuMessageReturnsSuccessMessagePost() {

    TimDepositController testTimDepositController =
        new TimDepositController(odeKafkaProperties, asn1CoderTopics, pojoTopics, jsonTopics,
            timIngestTrackerProperties, securityServicesProperties);

    String timToSubmit =
        "{\"request\": {\"rsus\": [{\"latitude\": 30.123456, \"longitude\": -100.12345, \"rsuId\": 123, \"route\": \"myroute\", \"milepost\": 10, \"rsuTarget\": \"172.0.0.1\", \"rsuRetries\": 3, \"rsuTimeout\": 5000, \"rsuIndex\": 7, \"rsuUsername\": \"myusername\", \"rsuPassword\": \"mypassword\"}], \"snmp\": {\"rsuid\": \"83\", \"msgid\": 31, \"mode\": 1, \"channel\": 183, \"interval\": 2000, \"deliverystart\": \"2024-05-13T14:30:00Z\", \"deliverystop\": \"2024-05-13T22:30:00Z\", \"enable\": 1, \"status\": 4}}, \"tim\": {\"msgCnt\": \"1\", \"timeStamp\": \"2024-05-10T19:01:22Z\", \"packetID\": \"123451234512345123\", \"urlB\": \"null\", \"dataframes\": [{\"startDateTime\": \"2024-05-13T20:30:05.014Z\", \"durationTime\": \"30\", \"doNotUse1\": 0, \"frameType\": \"advisory\", \"msgId\": {\"roadSignID\": {\"mutcdCode\": \"warning\", \"viewAngle\": \"1111111111111111\", \"position\": {\"latitude\": 30.123456, \"longitude\": -100.12345}}}, \"priority\": \"5\", \"doNotUse2\": 0, \"regions\": [{\"name\": \"I_myroute_RSU_172.0.0.1\", \"anchorPosition\": {\"latitude\": 30.123456, \"longitude\": -100.12345}, \"laneWidth\": \"50\", \"directionality\": \"3\", \"closedPath\": \"false\", \"description\": \"path\", \"path\": {\"scale\": 0, \"nodes\": [{\"delta\": \"node-LL\", \"nodeLat\": 0.0, \"nodeLong\": 0.0}, {\"delta\": \"node-LL\", \"nodeLat\": 0.0, \"nodeLong\": 0.0}], \"type\": \"ll\"}, \"direction\": \"0000000000010000\"}], \"doNotUse4\": 0, \"doNotUse3\": 0, \"content\": \"workZone\", \"items\": [\"771\"], \"url\": \"null\"}]}}";
    ResponseEntity<String> actualResponse = testTimDepositController.postTim(timToSubmit);
    Assertions.assertEquals("{\"success\":\"true\"}", actualResponse.getBody());

    // TODO: verify message is published to Kafka topics
  }

  /**
   * Helper method to create a consumer for String messages with Integer keys.
   *
   * @return a consumer for String messages
   */
  private Consumer<Integer, String> createInt2StrConsumer() {
    consumerCount++;
    var consumerProps =
        KafkaTestUtils.consumerProps("TimDepositControllerTest", "true", embeddedKafka);
    DefaultKafkaConsumerFactory<Integer, String> stringConsumerFactory =
        new DefaultKafkaConsumerFactory<>(consumerProps);
    return stringConsumerFactory.createConsumer(String.format("groupid%d", consumerCount),
        String.format("clientidsuffix%d", consumerCount));
  }

  /**
   * Helper method to create a consumer for OdeObject messages with Integer keys.
   *
   * @return a consumer for OdeObject messages
   */
  private Consumer<Integer, OdeObject> createInt2OdeObjConsumer() {
    consumerCount++;
    var consumerProps =
        KafkaTestUtils.consumerProps("TimDepositControllerTest", "true", embeddedKafka);
    DefaultKafkaConsumerFactory<Integer, OdeObject> pojoConsumerFactory =
        new DefaultKafkaConsumerFactory<>(consumerProps);
    return pojoConsumerFactory.createConsumer(String.format("groupid%d", consumerCount),
        String.format("clientidsuffix%d", consumerCount));
  }

  /**
   * Helper method to create a consumer for String messages with String keys.
   */
  private Consumer<String, String> createStr2StrConsumer() {
    consumerCount++;
    var consumerProps =
        KafkaTestUtils.consumerProps("TimDepositControllerTest", "true", embeddedKafka);
    DefaultKafkaConsumerFactory<String, String> stringConsumerFactory =
        new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(),
            new StringDeserializer());
    return stringConsumerFactory.createConsumer(String.format("groupid%d", consumerCount),
        String.format("clientidsuffix%d", consumerCount));
  }
}
