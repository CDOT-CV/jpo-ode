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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;
import mockit.Capturing;
import mockit.Expectations;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
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
import us.dot.its.jpo.ode.model.OdeObject;
import us.dot.its.jpo.ode.plugin.j2735.builders.TravelerMessageFromHumanToAsnConverter;
import us.dot.its.jpo.ode.security.SecurityServicesProperties;
import us.dot.its.jpo.ode.test.utilities.EmbeddedKafkaHolder;
import us.dot.its.jpo.ode.util.DateTimeUtils;
import us.dot.its.jpo.ode.util.JsonUtils.JsonUtilsException;


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
  void messageWithNoRSUsOrSDWShouldReturnWarning() throws IOException {
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
    Assertions.assertNotNull(singlePojoRecord.value());
    var singleRecord = KafkaTestUtils.getSingleRecord(stringConsumer, jsonTopics.getTimBroadcast());
    verifyMessageContentsJson(
        "messageWithNoRSUsOrSDWShouldReturnWarning_timBroadcast_expected.json",
        singleRecord.value());

    // cleanup
    stringConsumer.close();
    pojoConsumer.close();
  }

  @Test
  void failedObjectNodeConversionShouldReturnConvertingError(@Capturing
                                                             TravelerMessageFromHumanToAsnConverter capturingTravelerMessageFromHumanToAsnConverter)
      throws JsonUtilsException, TravelerMessageFromHumanToAsnConverter.NoncompliantFieldsException,
      IOException {
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
    Assertions.assertNotNull(singlePojoRecord.value());
    var singleRecord = KafkaTestUtils.getSingleRecord(stringConsumer, jsonTopics.getTimBroadcast());
    verifyMessageContentsJson(
        "failedObjectNodeConversionShouldReturnConvertingError_timBroadcast_expected.json",
        singleRecord.value());

    // cleanup
    stringConsumer.close();
    pojoConsumer.close();
  }

//  @Test
//  void failedXmlConversionShouldReturnConversionError(
//      @Capturing TimTransmogrifier capturingTimTransmogrifier)
//      throws XmlUtilsException, JsonUtilsException {
//    // prepare
//    odeKafkaProperties.setDisabledTopics(Set.of());
//    pojoTopics.setTimBroadcast(
//        "test.failedXmlConversionShouldReturnConversionError.timBroadcast.pojo");
//    jsonTopics.setTimBroadcast(
//        "test.failedXmlConversionShouldReturnConversionError.timBroadcast.json");
//    EmbeddedKafkaHolder.addTopics(pojoTopics.getTimBroadcast(), jsonTopics.getTimBroadcast());
//    DateTimeUtils.setClock(
//        Clock.fixed(Instant.parse("2018-03-13T01:07:11.120Z"), ZoneId.of("UTC")));
//    TimDepositController testTimDepositController =
//        new TimDepositController(odeKafkaProperties, asn1CoderTopics, pojoTopics, jsonTopics,
//            timIngestTrackerProperties, securityServicesProperties);
//
//    new Expectations() {
//      {
//        TimTransmogrifier.convertToXml((DdsAdvisorySituationData) any, (ObjectNode) any,
//            (OdeMsgMetadata) any, (SerialId) any);
//        result = new XmlUtilsException("testException123", null);
//      }
//    };
//    String requestBody =
//        "{\"request\":{\"rsus\":[],\"snmp\":{}},\"tim\":{\"msgCnt\":\"13\",\"timeStamp\":\"2017-03-13T01:07:11-05:00\"}}";
//
//    // execute
//    ResponseEntity<String> actualResponse = testTimDepositController.postTim(requestBody);
//
//    // verify
//    String expectedResponseBody =
//        "{\"error\":\"Error sending data to ASN.1 Encoder module: testException123\"}";
//    Assertions.assertEquals(expectedResponseBody, actualResponse.getBody());
//
//    var stringConsumer = createInt2StrConsumer();
//    var pojoConsumer = createInt2OdeObjConsumer();
//    embeddedKafka.consumeFromAnEmbeddedTopic(pojoConsumer, pojoTopics.getTimBroadcast());
//    embeddedKafka.consumeFromAnEmbeddedTopic(stringConsumer, jsonTopics.getTimBroadcast());
//    var singlePojoRecord =
//        KafkaTestUtils.getSingleRecord(pojoConsumer, pojoTopics.getTimBroadcast());
//    Assertions.assertNotNull(singlePojoRecord.value());
//    var singleRecord = KafkaTestUtils.getSingleRecord(stringConsumer, jsonTopics.getTimBroadcast());
//    Assertions.assertNotNull( // TODO: fix assertion failure
//        singleRecord.value()); // TODO: verify message contents instead of just existence
//
//    // cleanup
//    stringConsumer.close();
//    pojoConsumer.close();
//  }

  @Test
  void testSuccessfulMessageReturnsSuccessMessagePost() throws IOException {
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
    Assertions.assertNotNull(pojoTimBroadcastRecord.value());
    var jsonTimBroadcastRecord =
        KafkaTestUtils.getSingleRecord(jsonTimBroadcastConsumer, jsonTopics.getTimBroadcast());
    verifyMessageContentsJson(
        "successfulMessageReturnsSuccessMessagePost_timBroadcast_expected.json",
        jsonTimBroadcastRecord.value());
    var jsonJ2735TimBroadcastRecord = KafkaTestUtils.getSingleRecord(jsonJ2735TimBroadcastConsumer,
        jsonTopics.getJ2735TimBroadcast());
    verifyMessageContentsJson(
        "successfulMessageReturnsSuccessMessagePost_j2735TimBroadcast_expected.json",
        jsonJ2735TimBroadcastRecord.value());
    var jsonTimRecord = KafkaTestUtils.getSingleRecord(jsonTimConsumer, jsonTopics.getTim());
    verifyMessageContentsJson("successfulMessageReturnsSuccessMessagePost_tim_expected.json",
        jsonTimRecord.value());
    var asn1CoderEncoderInputRecord = KafkaTestUtils.getSingleRecord(asn1CoderEncoderInputConsumer,
        asn1CoderTopics.getEncoderInput());
    verifyMessageContentsXml("successfulMessageReturnsSuccessMessagePost_encoderInput_expected.xml",
        asn1CoderEncoderInputRecord.value());

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
    Assertions.assertNotNull(pojoTimBroadcastRecord.value());
    var jsonTimBroadcastRecord =
        KafkaTestUtils.getSingleRecord(jsonTimBroadcastConsumer, jsonTopics.getTimBroadcast());
    verifyMessageContentsJson(
        "successfulSdwRequestMessageReturnsSuccessMessagePost_timBroadcast_expected.json",
        jsonTimBroadcastRecord.value());
    var jsonJ2735TimBroadcastRecord = KafkaTestUtils.getSingleRecord(jsonJ2735TimBroadcastConsumer,
        jsonTopics.getJ2735TimBroadcast());
    verifyMessageContentsJson(
        "successfulSdwRequestMessageReturnsSuccessMessagePost_j2735TimBroadcast_expected.json",
        jsonJ2735TimBroadcastRecord.value());
    var jsonTimRecord = KafkaTestUtils.getSingleRecord(jsonTimConsumer, jsonTopics.getTim());
    verifyMessageContentsJson(
        "successfulSdwRequestMessageReturnsSuccessMessagePost_tim_expected.json",
        jsonTimRecord.value());
    var asn1CoderEncoderInputRecord = KafkaTestUtils.getSingleRecord(asn1CoderEncoderInputConsumer,
        asn1CoderTopics.getEncoderInput());
    verifyMessageContentsXml(
        "successfulSdwRequestMessageReturnsSuccessMessagePost_encoderInput_expected.xml",
        asn1CoderEncoderInputRecord.value());

    // cleanup
    pojoTimBroadcastConsumer.close();
    jsonTimBroadcastConsumer.close();
    jsonJ2735TimBroadcastConsumer.close();
    jsonTimConsumer.close();
    asn1CoderEncoderInputConsumer.close();
  }

  @Test
  void testSuccessfulMessageReturnsSuccessMessagePostWithOde() throws IOException {
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
    Assertions.assertNotNull(pojoTimBroadcastRecord.value());
    var jsonTimBroadcastRecord =
        KafkaTestUtils.getSingleRecord(jsonTimBroadcastConsumer, jsonTopics.getTimBroadcast());
    verifyMessageContentsJson(
        "successfulMessageReturnsSuccessMessagePostWithOde_timBroadcast_expected.json",
        jsonTimBroadcastRecord.value());
    var jsonJ2735TimBroadcastRecord = KafkaTestUtils.getSingleRecord(jsonJ2735TimBroadcastConsumer,
        jsonTopics.getJ2735TimBroadcast());
    verifyMessageContentsJson(
        "successfulMessageReturnsSuccessMessagePostWithOde_j2735TimBroadcast_expected.json",
        jsonJ2735TimBroadcastRecord.value());
    var jsonTimRecord = KafkaTestUtils.getSingleRecord(jsonTimConsumer, jsonTopics.getTim());
    verifyMessageContentsJson("successfulMessageReturnsSuccessMessagePostWithOde_tim_expected.json",
        jsonTimRecord.value());
    var asn1CoderEncoderInputRecord = KafkaTestUtils.getSingleRecord(asn1CoderEncoderInputConsumer,
        asn1CoderTopics.getEncoderInput());
    verifyMessageContentsXml(
        "successfulMessageReturnsSuccessMessagePostWithOde_encoderInput_expected.xml",
        asn1CoderEncoderInputRecord.value());

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
    Assertions.assertNotNull(pojoTimBroadcastRecord.value());
    var jsonTimBroadcastRecord =
        KafkaTestUtils.getSingleRecord(jsonTimBroadcastConsumer, jsonTopics.getTimBroadcast());
    Assertions.assertNotNull(
        jsonTimBroadcastRecord.value()); // TODO: verify message contents instead of just existence
    var jsonJ2735TimBroadcastRecord = KafkaTestUtils.getSingleRecord(jsonJ2735TimBroadcastConsumer,
        jsonTopics.getJ2735TimBroadcast());
    Assertions.assertNotNull(
        jsonJ2735TimBroadcastRecord.value()); // TODO: verify message contents instead of just existence
    var jsonTimRecord = KafkaTestUtils.getSingleRecord(jsonTimConsumer, jsonTopics.getTim());
    Assertions.assertNotNull(
        jsonTimRecord.value()); // TODO: verify message contents instead of just existence
    var asn1CoderEncoderInputRecord = KafkaTestUtils.getSingleRecord(asn1CoderEncoderInputConsumer,
        asn1CoderTopics.getEncoderInput());
    Assertions.assertNotNull(
        asn1CoderEncoderInputRecord.value()); // TODO: verify message contents instead of just existence

    // cleanup
    pojoTimBroadcastConsumer.close();
    jsonTimBroadcastConsumer.close();
    jsonJ2735TimBroadcastConsumer.close();
    jsonTimConsumer.close();
    asn1CoderEncoderInputConsumer.close();
  }

  @Test
  void testDepositingTimWithExtraProperties() {
    // prepare
    odeKafkaProperties.setDisabledTopics(Set.of());
    pojoTopics.setTimBroadcast("test.depositingTimWithExtraProperties.timBroadcast.pojo");
    jsonTopics.setTimBroadcast("test.depositingTimWithExtraProperties.timBroadcast.json");
    jsonTopics.setJ2735TimBroadcast("test.depositingTimWithExtraProperties.j2735TimBroadcast.json");
    jsonTopics.setTim("test.depositingTimWithExtraProperties.tim.json");
    asn1CoderTopics.setEncoderInput("test.depositingTimWithExtraProperties.encoderInput");
    EmbeddedKafkaHolder.addTopics(pojoTopics.getTimBroadcast(), jsonTopics.getTimBroadcast(),
        jsonTopics.getJ2735TimBroadcast(), jsonTopics.getTim(), asn1CoderTopics.getEncoderInput());
    DateTimeUtils.setClock(
        Clock.fixed(Instant.parse("2018-03-13T01:07:11.120Z"), ZoneId.of("UTC")));
    TimDepositController testTimDepositController =
        new TimDepositController(odeKafkaProperties, asn1CoderTopics, pojoTopics, jsonTopics,
            timIngestTrackerProperties, securityServicesProperties);
    String requestBody =
        "{\"request\":{\"rsus\":[],\"snmp\":{},\"randomProp1\":true,\"randomProp2\":\"hello world\"},\"tim\":{\"msgCnt\":\"13\",\"timeStamp\":\"2017-03-13T01:07:11-05:00\",\"randomProp3\":123,\"randomProp4\":{\"nestedProp1\":\"foo\",\"nestedProp2\":\"bar\"}}}";

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
    Assertions.assertNotNull(pojoTimBroadcastRecord.value());
    var jsonTimBroadcastRecord =
        KafkaTestUtils.getSingleRecord(jsonTimBroadcastConsumer, jsonTopics.getTimBroadcast());
    Assertions.assertNotNull(
        jsonTimBroadcastRecord.value()); // TODO: verify message contents instead of just existence
    var jsonJ2735TimBroadcastRecord = KafkaTestUtils.getSingleRecord(jsonJ2735TimBroadcastConsumer,
        jsonTopics.getJ2735TimBroadcast());
    Assertions.assertNotNull(
        jsonJ2735TimBroadcastRecord.value()); // TODO: verify message contents instead of just existence
    var jsonTimRecord = KafkaTestUtils.getSingleRecord(jsonTimConsumer, jsonTopics.getTim());
    Assertions.assertNotNull(
        jsonTimRecord.value()); // TODO: verify message contents instead of just existence
    var asn1CoderEncoderInputRecord = KafkaTestUtils.getSingleRecord(asn1CoderEncoderInputConsumer,
        asn1CoderTopics.getEncoderInput());
    Assertions.assertNotNull(
        asn1CoderEncoderInputRecord.value()); // TODO: verify message contents instead of just existence

    // cleanup
    pojoTimBroadcastConsumer.close();
    jsonTimBroadcastConsumer.close();
    jsonJ2735TimBroadcastConsumer.close();
    jsonTimConsumer.close();
    asn1CoderEncoderInputConsumer.close();
  }

  @Test
  void testSuccessfulTimIngestIsTracked() {
    // prepare
    odeKafkaProperties.setDisabledTopics(Set.of());
    pojoTopics.setTimBroadcast("test.successfulTimIngestIsTracked.timBroadcast.pojo");
    jsonTopics.setTimBroadcast("test.successfulTimIngestIsTracked.timBroadcast.json");
    jsonTopics.setJ2735TimBroadcast("test.successfulTimIngestIsTracked.j2735TimBroadcast.json");
    jsonTopics.setTim("test.successfulTimIngestIsTracked.tim.json");
    asn1CoderTopics.setEncoderInput("test.successfulTimIngestIsTracked.encoderInput");
    EmbeddedKafkaHolder.addTopics(pojoTopics.getTimBroadcast(), jsonTopics.getTimBroadcast(),
        jsonTopics.getJ2735TimBroadcast(), jsonTopics.getTim(), asn1CoderTopics.getEncoderInput());
    DateTimeUtils.setClock(
        Clock.fixed(Instant.parse("2018-03-13T01:07:11.120Z"), ZoneId.of("UTC")));
    TimDepositController testTimDepositController =
        new TimDepositController(odeKafkaProperties, asn1CoderTopics, pojoTopics, jsonTopics,
            timIngestTrackerProperties, securityServicesProperties);
    String requestBody =
        "{\"request\":{\"rsus\":[],\"snmp\":{},\"randomProp1\":true,\"randomProp2\":\"hello world\"},\"tim\":{\"msgCnt\":\"13\",\"timeStamp\":\"2017-03-13T01:07:11-05:00\",\"randomProp3\":123,\"randomProp4\":{\"nestedProp1\":\"foo\",\"nestedProp2\":\"bar\"}}}";
    long priorIngestCount = TimIngestTracker.getInstance().getTotalMessagesReceived();

    // execute
    ResponseEntity<String> actualResponse = testTimDepositController.postTim(requestBody);

    // verify
    String expectedResponseBody = "{\"success\":\"true\"}";
    Assertions.assertEquals(expectedResponseBody, actualResponse.getBody());
    Assertions.assertEquals(priorIngestCount + 1,
        TimIngestTracker.getInstance().getTotalMessagesReceived());

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
    Assertions.assertNotNull(pojoTimBroadcastRecord.value());
    var jsonTimBroadcastRecord =
        KafkaTestUtils.getSingleRecord(jsonTimBroadcastConsumer, jsonTopics.getTimBroadcast());
    Assertions.assertNotNull(
        jsonTimBroadcastRecord.value()); // TODO: verify message contents instead of just existence
    var jsonJ2735TimBroadcastRecord = KafkaTestUtils.getSingleRecord(jsonJ2735TimBroadcastConsumer,
        jsonTopics.getJ2735TimBroadcast());
    Assertions.assertNotNull(
        jsonJ2735TimBroadcastRecord.value()); // TODO: verify message contents instead of just existence
    var jsonTimRecord = KafkaTestUtils.getSingleRecord(jsonTimConsumer, jsonTopics.getTim());
    Assertions.assertNotNull(
        jsonTimRecord.value()); // TODO: verify message contents instead of just existence
    var asn1CoderEncoderInputRecord = KafkaTestUtils.getSingleRecord(asn1CoderEncoderInputConsumer,
        asn1CoderTopics.getEncoderInput());
    Assertions.assertNotNull(
        asn1CoderEncoderInputRecord.value()); // TODO: verify message contents instead of just existence

    // cleanup
    pojoTimBroadcastConsumer.close();
    jsonTimBroadcastConsumer.close();
    jsonJ2735TimBroadcastConsumer.close();
    jsonTimConsumer.close();
    asn1CoderEncoderInputConsumer.close();
  }

  // This serves as an integration test without mocking the TimTransmogrifier and XmlUtils
  @Test
  void testSuccessfulRsuMessageReturnsSuccessMessagePost() {
    // prepare
    odeKafkaProperties.setDisabledTopics(Set.of());
    pojoTopics.setTimBroadcast(
        "test.successfulRsuMessageReturnsSuccessMessagePost.timBroadcast.pojo");
    jsonTopics.setTimBroadcast(
        "test.successfulRsuMessageReturnsSuccessMessagePost.timBroadcast.json");
    jsonTopics.setJ2735TimBroadcast(
        "test.successfulRsuMessageReturnsSuccessMessagePost.j2735TimBroadcast.json");
    jsonTopics.setTim("test.successfulRsuMessageReturnsSuccessMessagePost.tim.json");
    asn1CoderTopics.setEncoderInput(
        "test.successfulRsuMessageReturnsSuccessMessagePost.encoderInput");
    EmbeddedKafkaHolder.addTopics(pojoTopics.getTimBroadcast(), jsonTopics.getTimBroadcast(),
        jsonTopics.getJ2735TimBroadcast(), jsonTopics.getTim(), asn1CoderTopics.getEncoderInput());
    DateTimeUtils.setClock(
        Clock.fixed(Instant.parse("2018-03-13T01:07:11.120Z"), ZoneId.of("UTC")));
    TimDepositController testTimDepositController =
        new TimDepositController(odeKafkaProperties, asn1CoderTopics, pojoTopics, jsonTopics,
            timIngestTrackerProperties, securityServicesProperties);
    String requestBody =
        "{\"request\": {\"rsus\": [{\"latitude\": 30.123456, \"longitude\": -100.12345, \"rsuId\": 123, \"route\": \"myroute\", \"milepost\": 10, \"rsuTarget\": \"172.0.0.1\", \"rsuRetries\": 3, \"rsuTimeout\": 5000, \"rsuIndex\": 7, \"rsuUsername\": \"myusername\", \"rsuPassword\": \"mypassword\"}], \"snmp\": {\"rsuid\": \"83\", \"msgid\": 31, \"mode\": 1, \"channel\": 183, \"interval\": 2000, \"deliverystart\": \"2024-05-13T14:30:00Z\", \"deliverystop\": \"2024-05-13T22:30:00Z\", \"enable\": 1, \"status\": 4}}, \"tim\": {\"msgCnt\": \"1\", \"timeStamp\": \"2024-05-10T19:01:22Z\", \"packetID\": \"123451234512345123\", \"urlB\": \"null\", \"dataframes\": [{\"startDateTime\": \"2024-05-13T20:30:05.014Z\", \"durationTime\": \"30\", \"doNotUse1\": 0, \"frameType\": \"advisory\", \"msgId\": {\"roadSignID\": {\"mutcdCode\": \"warning\", \"viewAngle\": \"1111111111111111\", \"position\": {\"latitude\": 30.123456, \"longitude\": -100.12345}}}, \"priority\": \"5\", \"doNotUse2\": 0, \"regions\": [{\"name\": \"I_myroute_RSU_172.0.0.1\", \"anchorPosition\": {\"latitude\": 30.123456, \"longitude\": -100.12345}, \"laneWidth\": \"50\", \"directionality\": \"3\", \"closedPath\": \"false\", \"description\": \"path\", \"path\": {\"scale\": 0, \"nodes\": [{\"delta\": \"node-LL\", \"nodeLat\": 0.0, \"nodeLong\": 0.0}, {\"delta\": \"node-LL\", \"nodeLat\": 0.0, \"nodeLong\": 0.0}], \"type\": \"ll\"}, \"direction\": \"0000000000010000\"}], \"doNotUse4\": 0, \"doNotUse3\": 0, \"content\": \"workZone\", \"items\": [\"771\"], \"url\": \"null\"}]}}";

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
    Assertions.assertNotNull(pojoTimBroadcastRecord.value());
    var jsonTimBroadcastRecord =
        KafkaTestUtils.getSingleRecord(jsonTimBroadcastConsumer, jsonTopics.getTimBroadcast());
    Assertions.assertNotNull(
        jsonTimBroadcastRecord.value()); // TODO: verify message contents instead of just existence
    var jsonJ2735TimBroadcastRecord = KafkaTestUtils.getSingleRecord(jsonJ2735TimBroadcastConsumer,
        jsonTopics.getJ2735TimBroadcast());
    Assertions.assertNotNull(
        jsonJ2735TimBroadcastRecord.value()); // TODO: verify message contents instead of just existence
    var jsonTimRecord = KafkaTestUtils.getSingleRecord(jsonTimConsumer, jsonTopics.getTim());
    Assertions.assertNotNull(
        jsonTimRecord.value()); // TODO: verify message contents instead of just existence
    var asn1CoderEncoderInputRecord = KafkaTestUtils.getSingleRecord(asn1CoderEncoderInputConsumer,
        asn1CoderTopics.getEncoderInput());
    Assertions.assertNotNull(
        asn1CoderEncoderInputRecord.value()); // TODO: verify message contents instead of just existence

    // cleanup
    pojoTimBroadcastConsumer.close();
    jsonTimBroadcastConsumer.close();
    jsonJ2735TimBroadcastConsumer.close();
    jsonTimConsumer.close();
    asn1CoderEncoderInputConsumer.close();
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

  /**
   * Helper method to retrieve the expected JSON contents from a file and verify that the actual JSON
   * contents are equivalent to the expected JSON contents.
   *
   * @param expectedJsonContentsFilename the name of the file containing the expected JSON contents
   * @param actualJsonContents           the actual JSON contents
   * @throws IOException if an I/O error occurs
   */
  private void verifyMessageContentsJson(String expectedJsonContentsFilename,
                                         String actualJsonContents) throws IOException {
    String baseDirectory = "src/test/resources/us/dot/its/jpo/ode/traveler/";
    String expectedJsonContents =
        new String(Files.readAllBytes(Paths.get(baseDirectory + expectedJsonContentsFilename)));
    verifyContentsAreEquivalentJson(expectedJsonContents, actualJsonContents);
  }

  /**
   * Helper method to verify that the contents of the actual JSON TIM message are equivalent to the
   * expected JSON TIM message, except for the stream id.
   */
  private void verifyContentsAreEquivalentJson(String expectedJsonTimContents,
                                               String actualJsonTimContents) {
    // verify stream id is different
    JSONObject expectedJsonTimBroadcastContentsJson = new JSONObject(expectedJsonTimContents);
    JSONObject actualJsonTimBroadcastContentsJson = new JSONObject(actualJsonTimContents);
    String actualStreamId =
        actualJsonTimBroadcastContentsJson.getJSONObject("metadata").getJSONObject("serialId")
            .getString("streamId");
    String expectedStreamId =
        expectedJsonTimBroadcastContentsJson.getJSONObject("metadata").getJSONObject("serialId")
            .getString("streamId");
    Assertions.assertNotEquals(expectedStreamId, actualStreamId);
    // remove stream id for comparison
    expectedJsonTimBroadcastContentsJson.getJSONObject("metadata").getJSONObject("serialId")
        .remove("streamId");
    actualJsonTimBroadcastContentsJson.getJSONObject("metadata").getJSONObject("serialId")
        .remove("streamId");
    JSONAssert.assertEquals(expectedJsonTimBroadcastContentsJson.toString(),
        actualJsonTimBroadcastContentsJson.toString(), false);
  }


  /**
   * Helper method to retrieve the expected XML contents from a file and verify that the actual XML
   * contents are equivalent to the expected XML contents.
   *
   * @param expectedXmlContentsFilename the name of the file containing the expected XML contents
   * @param actualXmlContents           the actual XML contents
   * @throws IOException if an I/O error occurs
   */
  private void verifyMessageContentsXml(String expectedXmlContentsFilename,
                                        String actualXmlContents) throws IOException {
    String baseDirectory = "src/test/resources/us/dot/its/jpo/ode/traveler/";
    String expectedXmlContents =
        new String(Files.readAllBytes(Paths.get(baseDirectory + expectedXmlContentsFilename)));
    verifyContentsAreEquivalentXml(expectedXmlContents, actualXmlContents);
  }

  /**
   * Helper method to verify that the contents of the actual XML TIM message are equivalent to the
   * expected XML TIM message, except for the stream id.
   */
  private void verifyContentsAreEquivalentXml(String expectedXmlTimContents,
                                              String actualXmlTimContents) {
    // verify stream id is different using string manipulation
    String actualStreamId =
        actualXmlTimContents.substring(actualXmlTimContents.indexOf("<streamId>"),
            actualXmlTimContents.indexOf("</streamId>") + "</streamId>".length());
    String expectedStreamId =
        expectedXmlTimContents.substring(expectedXmlTimContents.indexOf("<streamId>"),
            expectedXmlTimContents.indexOf("</streamId>") + "</streamId>".length());
    Assertions.assertNotEquals(expectedStreamId, actualStreamId);
    // remove stream id for comparison
    expectedXmlTimContents = expectedXmlTimContents.replace(expectedStreamId, "");
    actualXmlTimContents = actualXmlTimContents.replace(actualStreamId, "");
    Assertions.assertEquals(expectedXmlTimContents, actualXmlTimContents);
  }
}