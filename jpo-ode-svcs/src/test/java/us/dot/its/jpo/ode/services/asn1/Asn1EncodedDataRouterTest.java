/*============================================================================
 * Copyright 2018 572682
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package us.dot.its.jpo.ode.services.asn1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
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
import us.dot.its.jpo.ode.OdeTimJsonTopology;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.kafka.producer.KafkaProducerConfig;
import us.dot.its.jpo.ode.kafka.topics.Asn1CoderTopics;
import us.dot.its.jpo.ode.kafka.topics.JsonTopics;
import us.dot.its.jpo.ode.rsu.RsuDepositor;
import us.dot.its.jpo.ode.rsu.RsuProperties;
import us.dot.its.jpo.ode.security.ISecurityServicesClient;
import us.dot.its.jpo.ode.security.SecurityServicesProperties;
import us.dot.its.jpo.ode.test.utilities.EmbeddedKafkaHolder;
import us.dot.its.jpo.ode.wrapper.MessageConsumer;

@SpringBootTest(
    properties = {
        "ode.security-services.is-rsu-signing-enabled=false",
        "ode.security-services.is-sdw-signing-enabled=false",
        "ode.kafka.topics.json.tim-cert-expiration=topic.Asn1EncodedDataRouterTestTimCertExpiration",
        "ode.kafka.topics.json.tim-tmc-filtered=topic.Asn1EncodedDataRouterTestTimTmcFiltered",
        "ode.kafka.topics.asn1.encoder-input=topic.Asn1EncodedDataRouterTestEncoderInput",
        "ode.kafka.topics.asn1.encoder-output=topic.Asn1EncodedDataRouterTestEncoderOutput",
        "ode.kafka.topics.sdx-depositor.input=topic.Asn1EncodedDataRouterTestSDXDepositor"
    },
    classes = {
        KafkaProducerConfig.class,
        KafkaProperties.class,
        Asn1CoderTopics.class,
        JsonTopics.class,
        SecurityServicesProperties.class,
        RsuProperties.class,
        OdeKafkaProperties.class
    }
)
@EnableConfigurationProperties
@DirtiesContext
class Asn1EncodedDataRouterTest {

  @Autowired
  Asn1CoderTopics asn1CoderTopics;
  @Autowired
  JsonTopics jsonTopics;
  @Autowired
  SecurityServicesProperties securityServicesProperties;
  @Autowired
  OdeKafkaProperties odeKafkaProperties;
  @Value("${ode.kafka.topics.sdx-depositor.input}")
  String sdxDepositorTopic;
  @Autowired
  KafkaTemplate<String, String> kafkaTemplate;
  @Mock
  RsuDepositor mockRsuDepositor;

  EmbeddedKafkaBroker embeddedKafka = EmbeddedKafkaHolder.getEmbeddedKafka();

  @Test
  void processSignedMessage_depositsToSdxTopicAndTimTmcFiltered()
      throws IOException, InterruptedException {

    String[] topicsForConsumption = {
        asn1CoderTopics.getEncoderInput(),
        jsonTopics.getTimTmcFiltered(),
        sdxDepositorTopic
    };
    EmbeddedKafkaHolder.addTopics(topicsForConsumption);
    EmbeddedKafkaHolder.addTopics(asn1CoderTopics.getEncoderOutput(), jsonTopics.getTim());

    var odeTimJsonTopology = new OdeTimJsonTopology(odeKafkaProperties, jsonTopics.getTim());
    var mockSecServClient = mock(ISecurityServicesClient.class);
    securityServicesProperties.setIsSdwSigningEnabled(true);
    Asn1EncodedDataRouter encoderRouter = new Asn1EncodedDataRouter(
        odeKafkaProperties,
        asn1CoderTopics,
        jsonTopics,
        securityServicesProperties,
        odeTimJsonTopology,
        mockRsuDepositor,
        mockSecServClient,
        sdxDepositorTopic
    );

    MessageConsumer<String, String> encoderConsumer = MessageConsumer.defaultStringMessageConsumer(
        embeddedKafka.getBrokersAsString(),
        "processSignedMessage_depositsToSdxTopicAndTimTmcFiltered", encoderRouter);

    encoderConsumer.setName("Asn1EncoderConsumer");
    encoderRouter.start(encoderConsumer, asn1CoderTopics.getEncoderOutput());

    // Wait for encoderRouter to connect to the broker otherwise the test will fail :(
    Thread.sleep(2000);

    var classLoader = getClass().getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(
        "us/dot/its/jpo/ode/services/asn1/expected-asn1-encoded-router-tim-json.json");
    assert inputStream != null;
    var odeJsonTim = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    // send to tim topic so that the OdeTimJsonTopology ktable has the correct record to return
    var streamId = "266e6742-40fb-4c9e-a6b0-72ed2dddddfe";
    kafkaTemplate.send(jsonTopics.getTim(), streamId, odeJsonTim);

    inputStream = classLoader
        .getResourceAsStream(
            "us/dot/its/jpo/ode/services/asn1/asn1-encoder-output-unsigned-tim.xml");
    assert inputStream != null;
    var input = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    var completableFuture = kafkaTemplate.send(asn1CoderTopics.getEncoderOutput(), input);
    Awaitility.await().until(completableFuture::isDone);

    var consumerProps = KafkaTestUtils.consumerProps(
        "processSignedMessage_depositsToSdxTopicAndTimTmcFiltered-test", "false", embeddedKafka);
    var consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps,
        new StringDeserializer(), new StringDeserializer());
    var testConsumer = consumerFactory.createConsumer();
    embeddedKafka.consumeFromEmbeddedTopics(testConsumer, topicsForConsumption);

    inputStream = classLoader.getResourceAsStream(
        "us/dot/its/jpo/ode/services/asn1/expected-asn1-encoded-router-sdx-deposit.json");
    assert inputStream != null;
    var expected = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

    var records = KafkaTestUtils.getRecords(testConsumer);
    var sdxDepositorRecord = records
        .records(sdxDepositorTopic);
    for (var consumerRecord : sdxDepositorRecord) {
      if (consumerRecord.value().contains(streamId)) {
        assertEquals(expected, consumerRecord.value());
      }
    }

    inputStream = classLoader.getResourceAsStream(
        "us/dot/its/jpo/ode/services/asn1/expected-tim-tmc-filtered.json");
    assert inputStream != null;
    var expectedTimTmcFiltered = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

    for (var consumerRecord : records.records(jsonTopics.getTimTmcFiltered())) {
      if (consumerRecord.value().contains(streamId)) {
        assertEquals(expectedTimTmcFiltered, consumerRecord.value());
      }
    }
  }

  @Test
  void processSNMPDepositOnly()
      throws IOException, InterruptedException {
    String[] topicsForConsumption = {
        asn1CoderTopics.getEncoderInput(),
        jsonTopics.getTimCertExpiration(),
        jsonTopics.getTimTmcFiltered()
    };
    EmbeddedKafkaHolder.addTopics(topicsForConsumption);
    EmbeddedKafkaHolder.addTopics(asn1CoderTopics.getEncoderOutput(), jsonTopics.getTim());

    securityServicesProperties.setIsSdwSigningEnabled(true);
    var odeTimJsonTopology = new OdeTimJsonTopology(odeKafkaProperties, jsonTopics.getTim());
    var mockSecServClient = new ISecurityServicesClient() {
      @Override
      public String signMessage(String message, int sigValidityOverride) {
        JSONObject json = new JSONObject();
        JSONObject result = new JSONObject();
        result.put("message-signed", "<%s>".formatted(message));
        result.put("message-expiry", "123124124124124141");
        json.put("result", result);
        return json.toString();
      }
    };

    Asn1EncodedDataRouter encoderRouter = new Asn1EncodedDataRouter(
        odeKafkaProperties,
        asn1CoderTopics,
        jsonTopics,
        securityServicesProperties,
        odeTimJsonTopology,
        mockRsuDepositor,
        mockSecServClient,
        sdxDepositorTopic
    );
    MessageConsumer<String, String> encoderConsumer = MessageConsumer.defaultStringMessageConsumer(
        embeddedKafka.getBrokersAsString(), "processSNMPDepositOnly-default", encoderRouter);

    encoderConsumer.setName("Asn1EncoderConsumer");
    encoderRouter.start(encoderConsumer, asn1CoderTopics.getEncoderOutput());

    // Wait for encoderRouter to connect to the broker otherwise the test will fail :(
    Thread.sleep(2000);

    var classLoader = getClass().getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(
        "us/dot/its/jpo/ode/services/asn1/expected-asn1-encoded-router-tim-json.json");
    assert inputStream != null;
    var odeJsonTim = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    // send to tim topic so that the OdeTimJsonTopology ktable has the correct record to return
    var streamId = UUID.randomUUID().toString();
    odeJsonTim = odeJsonTim.replaceAll("266e6742-40fb-4c9e-a6b0-72ed2dddddfe", streamId);
    kafkaTemplate.send(jsonTopics.getTim(), streamId, odeJsonTim);

    inputStream = classLoader.getResourceAsStream(
        "us/dot/its/jpo/ode/services/asn1/asn1-encoder-output-unsigned-tim-no-advisory-data.xml");
    assert inputStream != null;
    var input = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    input = input.replaceAll("<streamId>.*?</streamId>", "<streamId>" + streamId + "</streamId>");
    kafkaTemplate.send(asn1CoderTopics.getEncoderOutput(), input);

    var consumerProps = KafkaTestUtils.consumerProps(
        "processSNMPDepositOnly", "false", embeddedKafka);
    var consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps,
        new StringDeserializer(), new StringDeserializer());
    var testConsumer = consumerFactory.createConsumer();
    embeddedKafka.consumeFromEmbeddedTopics(testConsumer, topicsForConsumption);

    inputStream = classLoader.getResourceAsStream(
        "us/dot/its/jpo/ode/services/asn1/expected-tim-cert-expired.json");
    assert inputStream != null;
    var expectedTimCertExpiry = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    var timCertExpirationRecord =
        KafkaTestUtils.getSingleRecord(testConsumer, jsonTopics.getTimCertExpiration());
    assertEquals(expectedTimCertExpiry, timCertExpirationRecord.value());

    inputStream = classLoader.getResourceAsStream(
        "us/dot/its/jpo/ode/services/asn1/expected-tim-tmc-filtered.json");
    assert inputStream != null;
    var expectedTimTmcFiltered = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    var records = KafkaTestUtils.getRecords(testConsumer);
    expectedTimTmcFiltered = expectedTimTmcFiltered.replaceAll("266e6742-40fb-4c9e-a6b0-72ed2dddddfe", streamId);
    for (var consumerRecord : records.records(jsonTopics.getTimTmcFiltered())) {
      if (consumerRecord.value().contains(streamId)) {
        assertEquals(expectedTimTmcFiltered, consumerRecord.value());
      }
    }

    inputStream = classLoader.getResourceAsStream(
        "us/dot/its/jpo/ode/services/asn1/expected-asn1-encoded-router-snmp-deposit.xml");
    assert inputStream != null;
    var expectedEncoderInput = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    var expectedEncoderInputWithStableFieldsOnly = expectedEncoderInput
        .replaceAll("<streamId>.*?</streamId>", "")
        .replaceAll("<requestID>.*?</requestID>", "")
        .replaceAll("<odeReceivedAt>.*?</odeReceivedAt>", "")
        .replaceAll("<asdmID>.*?</asdmID>", "");
    for (var consumerRecord : records.records(asn1CoderTopics.getEncoderInput())) {
      if (consumerRecord.value().contains(streamId)) {
        var encoderInputWithStableFieldsOnly = consumerRecord.value()
            .replaceAll("<streamId>.*?</streamId>", "")
            .replaceAll("<requestID>.*?</requestID>", "")
            .replaceAll("<odeReceivedAt>.*?</odeReceivedAt>", "")
            .replaceAll("<asdmID>.*?</asdmID>", "");
        assertEquals(expectedEncoderInputWithStableFieldsOnly, encoderInputWithStableFieldsOnly);
      }
    }
  }

  @Test
  void processEncodedTimUnsecured()
      throws IOException, InterruptedException {
    String[] topicsForConsumption = {
        asn1CoderTopics.getEncoderInput(),
        jsonTopics.getTimTmcFiltered()
    };
    EmbeddedKafkaHolder.addTopics(topicsForConsumption);
    EmbeddedKafkaHolder.addTopics(asn1CoderTopics.getEncoderOutput(), jsonTopics.getTim());

    var odeTimJsonTopology = new OdeTimJsonTopology(odeKafkaProperties, jsonTopics.getTim());
    var mockSecServClient = new ISecurityServicesClient() {
      @Override
      public String signMessage(String message, int sigValidityOverride) {
        JSONObject json = new JSONObject();
        JSONObject result = new JSONObject();
        result.put("message-signed", "<%s>".formatted(message));
        result.put("message-expiry", "123124124124124141");
        json.put("result", result);
        return json.toString();
      }
    };

    securityServicesProperties.setIsSdwSigningEnabled(false);
    securityServicesProperties.setIsRsuSigningEnabled(false);
    Asn1EncodedDataRouter encoderRouter = new Asn1EncodedDataRouter(
        odeKafkaProperties,
        asn1CoderTopics,
        jsonTopics,
        securityServicesProperties,
        odeTimJsonTopology,
        mockRsuDepositor,
        mockSecServClient,
        sdxDepositorTopic
    );
    MessageConsumer<String, String> encoderConsumer = MessageConsumer.defaultStringMessageConsumer(
        embeddedKafka.getBrokersAsString(), this.getClass().getSimpleName(), encoderRouter);

    encoderConsumer.setName("Asn1EncoderConsumer");
    encoderRouter.start(encoderConsumer, asn1CoderTopics.getEncoderOutput());

    // Wait for encoderRouter to connect to the broker otherwise the test will fail :(
    Thread.sleep(2000);

    var classLoader = getClass().getClassLoader();
    InputStream inputStream = classLoader.getResourceAsStream(
        "us/dot/its/jpo/ode/services/asn1/expected-asn1-encoded-router-tim-json.json");
    assert inputStream != null;
    var odeJsonTim = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    // send to tim topic so that the OdeTimJsonTopology ktable has the correct record to return
    var streamId = UUID.randomUUID().toString();
    odeJsonTim = odeJsonTim.replaceAll("266e6742-40fb-4c9e-a6b0-72ed2dddddfe", streamId);
    kafkaTemplate.send(jsonTopics.getTim(), streamId, odeJsonTim);

    inputStream = classLoader.getResourceAsStream(
        "us/dot/its/jpo/ode/services/asn1/asn1-encoder-output-unsigned-tim.xml");
    assert inputStream != null;
    var input = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    input = input.replaceAll("<streamId>.*?</streamId>", "<streamId>" + streamId + "</streamId>");
    kafkaTemplate.send(asn1CoderTopics.getEncoderOutput(), input);

    var consumerProps = KafkaTestUtils.consumerProps(
        "processEncodedTimUnsecured", "false", embeddedKafka);
    var consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps,
        new StringDeserializer(), new StringDeserializer());
    var testConsumer = consumerFactory.createConsumer();
    embeddedKafka.consumeFromEmbeddedTopics(testConsumer, topicsForConsumption);

    inputStream = classLoader.getResourceAsStream(
        "us/dot/its/jpo/ode/services/asn1/expected-asn1-encoded-router-sdx-deposit.json");
    assert inputStream != null;
    var expected = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

    var records = KafkaTestUtils.getRecords(testConsumer);
    var sdxDepositorRecord = records
        .records(sdxDepositorTopic);
    for (var consumerRecord : sdxDepositorRecord) {
      if (consumerRecord.value().contains(streamId)) {
        assertEquals(expected, consumerRecord.value());
      }
    }

    inputStream = classLoader.getResourceAsStream(
        "us/dot/its/jpo/ode/services/asn1/expected-tim-tmc-filtered.json");
    assert inputStream != null;
    var expectedTimTmcFiltered = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    expectedTimTmcFiltered = expectedTimTmcFiltered.replaceAll("266e6742-40fb-4c9e-a6b0-72ed2dddddfe", streamId);

    for (var consumerRecord : records.records(jsonTopics.getTimTmcFiltered())) {
      if (consumerRecord.value().contains(streamId)) {
        assertEquals(expectedTimTmcFiltered, consumerRecord.value());
      }
    }
  }

}
