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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import us.dot.its.jpo.ode.OdeTimJsonTopology;
import us.dot.its.jpo.ode.config.SerializationConfig;
import us.dot.its.jpo.ode.http.WebClientConfig;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.kafka.TestKafkaStreamsConfig;
import us.dot.its.jpo.ode.kafka.producer.KafkaProducerConfig;
import us.dot.its.jpo.ode.kafka.topics.Asn1CoderTopics;
import us.dot.its.jpo.ode.kafka.topics.JsonTopics;
import us.dot.its.jpo.ode.rsu.RsuDepositor;
import us.dot.its.jpo.ode.rsu.RsuProperties;
import us.dot.its.jpo.ode.security.SecurityServicesClient;
import us.dot.its.jpo.ode.security.SecurityServicesProperties;
import us.dot.its.jpo.ode.security.models.SignatureResultModel;
import us.dot.its.jpo.ode.test.utilities.EmbeddedKafkaHolder;

@Slf4j
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
        OdeKafkaProperties.class,
        KafkaProducerConfig.class,
        SerializationConfig.class,
        KafkaProperties.class,
        TestKafkaStreamsConfig.class,
        Asn1CoderTopics.class,
        JsonTopics.class,
        SecurityServicesProperties.class,
        RsuProperties.class,
        Asn1EncodedDataRouterTest.MockSecurityServicesClient.class,
        WebClientConfig.class
    }
)
@EnableConfigurationProperties
@DirtiesContext
@ActiveProfiles("test")
class Asn1EncodedDataRouterTest {

  private final EmbeddedKafkaBroker embeddedKafka = EmbeddedKafkaHolder.getEmbeddedKafka();
  @Autowired
  Asn1CoderTopics asn1CoderTopics;
  @Autowired
  JsonTopics jsonTopics;
  @Autowired
  SecurityServicesProperties securityServicesProperties;
  @Autowired
  KafkaTemplate<String, String> kafkaTemplate;
  @Autowired
  OdeTimJsonTopology odeTimJsonTopology;
  @Autowired
  ObjectMapper objectMapper;
  @Autowired
  MockSecurityServicesClient secServicesClient;

  @Value("${ode.kafka.topics.sdx-depositor.input}")
  String sdxDepositorTopic;

  @Mock
  RsuDepositor mockRsuDepositor;
  @Autowired
  private XmlMapper xmlMapper;

  private static String stripGeneratedFields(String expectedEncoderInput) {
    return expectedEncoderInput
        .replaceAll("<streamId>.*?</streamId>", "")
        .replaceAll("<requestID>.*?</requestID>", "")
        .replaceAll("<odeReceivedAt>.*?</odeReceivedAt>", "")
        .replaceAll("<asdmID>.*?</asdmID>", "");
  }

  private static String replaceStreamId(String input, String streamId) {
    return input.replaceAll("<streamId>.*?</streamId>", "<streamId>" + streamId + "</streamId>");
  }

  private static String loadResourceString(String name)
      throws IOException {
    String resourcePackagePath = "us/dot/its/jpo/ode/services/asn1/";
    InputStream inputStream;
    inputStream = Asn1EncodedDataRouterTest.class.getClassLoader()
        .getResourceAsStream(resourcePackagePath + name);
    assert inputStream != null;
    return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
  }

  @Test
  void processSignedMessage() throws IOException {
    String[] topicsForConsumption = {
        asn1CoderTopics.getEncoderInput(),
        jsonTopics.getTimTmcFiltered(),
        sdxDepositorTopic
    };
    EmbeddedKafkaHolder.addTopics(topicsForConsumption);

    securityServicesProperties.setIsSdwSigningEnabled(true);
    Asn1EncodedDataRouter encoderRouter = new Asn1EncodedDataRouter(
        asn1CoderTopics,
        jsonTopics,
        securityServicesProperties,
        odeTimJsonTopology,
        mockRsuDepositor,
        secServicesClient,
        kafkaTemplate, sdxDepositorTopic,
        objectMapper,
        xmlMapper);

    final var container = setupListenerContainer(encoderRouter,
        "processSignedMessage"
    );

    var odeJsonTim = loadResourceString("expected-asn1-encoded-router-tim-json.json");

    // send to tim topic so that the OdeTimJsonTopology k-table has the correct record to return
    var streamId = "266e6742-40fb-4c9e-a6b0-72ed2dddddfe";
    kafkaTemplate.send(jsonTopics.getTim(), streamId, odeJsonTim);

    var input = loadResourceString("asn1-encoder-output-unsigned-tim.xml");
    var completableFuture = kafkaTemplate.send(asn1CoderTopics.getEncoderOutput(), input);
    Awaitility.await().until(completableFuture::isDone);

    var testConsumer =
        createTestConsumer("processSignedMessage");
    embeddedKafka.consumeFromEmbeddedTopics(testConsumer, topicsForConsumption);

    var expected = loadResourceString("expected-asn1-encoded-router-sdx-deposit.json");

    var records = KafkaTestUtils.getRecords(testConsumer);
    var sdxDepositorRecord = records.records(sdxDepositorTopic);
    var foundValidRecord = false;
    for (var consumerRecord : sdxDepositorRecord) {
      if (consumerRecord.value().equals(expected)) {
        foundValidRecord = true;
      }
    }
    assertTrue(foundValidRecord);
    container.stop();
    log.debug("processSignedMessage container stopped");
  }

  @Test
  void processUnsignedMessage() throws IOException {
    String[] topicsForConsumption = {
        asn1CoderTopics.getEncoderInput(),
        jsonTopics.getTimCertExpiration(),
        jsonTopics.getTimTmcFiltered()
    };
    EmbeddedKafkaHolder.addTopics(topicsForConsumption);

    securityServicesProperties.setIsSdwSigningEnabled(true);
    securityServicesProperties.setIsRsuSigningEnabled(true);
    Asn1EncodedDataRouter encoderRouter = new Asn1EncodedDataRouter(
        asn1CoderTopics,
        jsonTopics,
        securityServicesProperties,
        odeTimJsonTopology,
        mockRsuDepositor,
        secServicesClient,
        kafkaTemplate, sdxDepositorTopic,
        objectMapper,
        xmlMapper);

    final var container = setupListenerContainer(encoderRouter, "processUnsignedMessage");

    var odeJsonTim = loadResourceString("expected-asn1-encoded-router-tim-json.json");
    // send to tim topic so that the OdeTimJsonTopology k-table has the correct record to return
    var streamId = UUID.randomUUID().toString();
    odeJsonTim = odeJsonTim.replaceAll("266e6742-40fb-4c9e-a6b0-72ed2dddddfe", streamId);
    var topologySendFuture = kafkaTemplate.send(jsonTopics.getTim(), streamId, odeJsonTim);
    Awaitility.await().until(topologySendFuture::isDone);

    var input = loadResourceString("asn1-encoder-output-unsigned-tim-no-advisory-data.xml");
    input = replaceStreamId(input, streamId);

    var completableFuture = kafkaTemplate.send(asn1CoderTopics.getEncoderOutput(), input);
    Awaitility.await().until(completableFuture::isDone);

    var consumerProps = KafkaTestUtils.consumerProps(
        "processUnsignedMessage", "false", embeddedKafka);
    var consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps,
        new StringDeserializer(), new StringDeserializer());

    var timCertConsumer =
        consumerFactory.createConsumer("timCertExpiration", "processUnsignedMessage");
    embeddedKafka.consumeFromAnEmbeddedTopic(timCertConsumer, jsonTopics.getTimCertExpiration());
    var expectedTimCertExpiry = loadResourceString("expected-tim-cert-expired.json");
    var timCertExpirationRecord =
        KafkaTestUtils.getSingleRecord(timCertConsumer, jsonTopics.getTimCertExpiration());
    assertEquals(expectedTimCertExpiry, timCertExpirationRecord.value());

    var timTmcFilteredConsumer =
        consumerFactory.createConsumer("timTmcFiltered", "processUnsignedMessage");
    embeddedKafka.consumeFromAnEmbeddedTopic(timTmcFilteredConsumer,
        jsonTopics.getTimTmcFiltered());
    var expectedTimTmcFiltered = loadResourceString("expected-tim-tmc-filtered.json");
    var records = KafkaTestUtils.getRecords(timTmcFilteredConsumer);
    expectedTimTmcFiltered =
        expectedTimTmcFiltered.replaceAll("266e6742-40fb-4c9e-a6b0-72ed2dddddfe", streamId);
    var foundValidRecord = false;
    for (var consumerRecord : records.records(jsonTopics.getTimTmcFiltered())) {
      if (consumerRecord.value().contains(streamId)) {
        assertEquals(expectedTimTmcFiltered, consumerRecord.value());
        foundValidRecord = true;
      }
    }
    assertTrue(foundValidRecord);

    var encoderInputConsumer =
        consumerFactory.createConsumer("encoderInput", "processUnsignedMessage");
    embeddedKafka.consumeFromAnEmbeddedTopic(encoderInputConsumer,
        asn1CoderTopics.getEncoderInput());
    var expectedEncoderInput = loadResourceString("expected-asn1-encoded-router-snmp-deposit.xml");
    var expectedEncoderInputWithStableFieldsOnly = stripGeneratedFields(expectedEncoderInput);
    var encoderInputRecords = KafkaTestUtils.getRecords(encoderInputConsumer);
    for (var consumerRecord : encoderInputRecords.records(asn1CoderTopics.getEncoderInput())) {
      var encoderInputWithStableFieldsOnly = stripGeneratedFields(consumerRecord.value());
      assertEquals(expectedEncoderInputWithStableFieldsOnly, encoderInputWithStableFieldsOnly);
    }
    container.stop();
    log.debug("processUnsignedMessage container stopped");
  }

  @Test
  void processEncodedTimUnsigned() throws IOException {
    String[] topicsForConsumption = {
        asn1CoderTopics.getEncoderInput(),
        jsonTopics.getTimTmcFiltered()
    };
    EmbeddedKafkaHolder.addTopics(topicsForConsumption);

    securityServicesProperties.setIsSdwSigningEnabled(false);
    securityServicesProperties.setIsRsuSigningEnabled(false);
    Asn1EncodedDataRouter encoderRouter = new Asn1EncodedDataRouter(
        asn1CoderTopics,
        jsonTopics,
        securityServicesProperties,
        odeTimJsonTopology,
        mockRsuDepositor,
        secServicesClient,
        kafkaTemplate,
        sdxDepositorTopic,
        objectMapper,
        xmlMapper);

    final var container = setupListenerContainer(encoderRouter, "processEncodedTimUnsigned");
    var odeJsonTim = loadResourceString("expected-asn1-encoded-router-tim-json.json");

    // send to tim topic so that the OdeTimJsonTopology k-table has the correct record to return
    var streamId = UUID.randomUUID().toString();
    odeJsonTim = odeJsonTim.replaceAll("266e6742-40fb-4c9e-a6b0-72ed2dddddfe", streamId);
    kafkaTemplate.send(jsonTopics.getTim(), streamId, odeJsonTim);

    var input = loadResourceString("asn1-encoder-output-unsigned-tim.xml");
    input = replaceStreamId(input, streamId);
    kafkaTemplate.send(asn1CoderTopics.getEncoderOutput(), input);

    var testConsumer = createTestConsumer("processEncodedTimUnsigned");
    embeddedKafka.consumeFromEmbeddedTopics(testConsumer, topicsForConsumption);

    var expected = loadResourceString("expected-asn1-encoded-router-sdx-deposit.json");

    var records = KafkaTestUtils.getRecords(testConsumer);
    var sdxDepositorRecord = records
        .records(sdxDepositorTopic);
    for (var consumerRecord : sdxDepositorRecord) {
      if (consumerRecord.value().contains(streamId)) {
        assertEquals(expected, consumerRecord.value());
      }
    }

    var expectedTimTmcFiltered = loadResourceString("expected-tim-tmc-filtered.json");
    expectedTimTmcFiltered =
        expectedTimTmcFiltered.replaceAll("266e6742-40fb-4c9e-a6b0-72ed2dddddfe", streamId);

    var foundValidRecord = false;
    for (var consumerRecord : records.records(jsonTopics.getTimTmcFiltered())) {
      if (consumerRecord.value().contains(streamId)) {
        assertEquals(expectedTimTmcFiltered, consumerRecord.value());
        foundValidRecord = true;
      }
    }
    assertTrue(foundValidRecord);
    container.stop();
    log.debug("processEncodedTimUnsigned container stopped");
  }

  private KafkaMessageListenerContainer<String, String> setupListenerContainer(
      Asn1EncodedDataRouter encoderRouter,
      String containerName) {
    var consumerProps = KafkaTestUtils.consumerProps(containerName, "false", embeddedKafka);
    DefaultKafkaConsumerFactory<String, String> consumerFactory =
        new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer());
    ContainerProperties containerProperties = new ContainerProperties(asn1CoderTopics.getEncoderOutput());
    KafkaMessageListenerContainer<String, String> container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
    container.setupMessageListener(
        (MessageListener<String, String>) consumerRecord -> {
          try {
            encoderRouter.listen(consumerRecord);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
    );
    container.setBeanName(containerName);
    container.start();
    ContainerTestUtils.waitForAssignment(container, embeddedKafka.getPartitionsPerTopic());
    log.debug("{} started", containerName);
    return container;
  }

  private Consumer<String, String> createTestConsumer(String group) {
    var consumerProps = KafkaTestUtils.consumerProps(
        group, "false", embeddedKafka);
    var consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps,
        new StringDeserializer(), new StringDeserializer());
    return consumerFactory.createConsumer();
  }

  @Service
  @Profile("test")
  static class MockSecurityServicesClient extends SecurityServicesClient {
    private static final Clock clock = Clock.fixed(Instant.parse("2024-03-08T16:37:05.414Z"), ZoneId.of("UTC"));

    public MockSecurityServicesClient(RestTemplate restTemplate, SecurityServicesProperties securityServicesProperties) {
      super(restTemplate, securityServicesProperties);
    }

    @Override
    public SignatureResultModel signMessage(String message, int sigValidityOverride) throws RestClientException {
      var signatureResponse = new SignatureResultModel();
      signatureResponse.setMessageSigned("<%s>".formatted(message));
      signatureResponse.setMessageExpiry(clock.instant().getEpochSecond() + 1000);
      return signatureResponse;
    }
  }
}
