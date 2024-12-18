/*******************************************************************************
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
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
import us.dot.its.jpo.ode.kafka.topics.SDXDepositorTopics;
import us.dot.its.jpo.ode.rsu.RsuDepositor;
import us.dot.its.jpo.ode.rsu.RsuProperties;
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
        "ode.kafka.topics.asn1.encoder-output=topic.Asn1EncodedDataRouterTestEncoderOutput"
    },
    classes = {
        KafkaProducerConfig.class,
        KafkaProperties.class,
        Asn1CoderTopics.class,
        SDXDepositorTopics.class,
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
  RsuProperties rsuProperties;
  @Autowired
  OdeKafkaProperties odeKafkaProperties;
  @Autowired
  SDXDepositorTopics sdxDepositorTopics;
  @Autowired
  KafkaTemplate<String, String> kafkaTemplate;
  @Mock
  RsuDepositor mockRsuDepositor;
  @Mock
  OdeTimJsonTopology mockOdeTimJsonTopology;

  EmbeddedKafkaBroker embeddedKafka = EmbeddedKafkaHolder.getEmbeddedKafka();

  @Test
  void processSNMPDeposit() throws IOException {
    //`dataSigningEnabledRSU` AND request.getRsus() is not null AND recordGeneratedBy set to "TMC"
    //			- produced to getTimCertExpiration topic
    //			- produced to getEncoderInput topic
    //			- produced to getTimTmcFiltered
    String[] topics = {
        asn1CoderTopics.getEncoderInput(),
        jsonTopics.getTimCertExpiration(),
        asn1CoderTopics.getEncoderOutput(),
        jsonTopics.getTimTmcFiltered()
    };
    EmbeddedKafkaHolder.addTopics(topics);
//    EmbeddedKafkaHolder.addTopics(asn1CoderTopics.getEncoderOutput());

    // mock RsuDepositor
    // mock Asn1CommandManager?

    Asn1EncodedDataRouter encoderRouter = new Asn1EncodedDataRouter(
        odeKafkaProperties,
        asn1CoderTopics,
        jsonTopics,
        sdxDepositorTopics,
        securityServicesProperties,
        mockRsuDepositor,
        mockOdeTimJsonTopology);

    MessageConsumer<String, String> encoderConsumer = MessageConsumer.defaultStringMessageConsumer(
        embeddedKafka.getBrokersAsString(), this.getClass().getSimpleName(), encoderRouter);

    encoderConsumer.setName("Asn1EncoderConsumer");
    encoderRouter.start(encoderConsumer, asn1CoderTopics.getEncoderOutput());

    InputStream inputStream = getClass().getClassLoader()
        .getResourceAsStream("us/dot/its/jpo/ode/services/asn1/asn1-encoder-output-unsigned-tim.xml");
    assert inputStream != null;
    var input = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    kafkaTemplate.send(asn1CoderTopics.getEncoderOutput(), input);

    var consumerProps = KafkaTestUtils.consumerProps(
        "Asn1EncodedDataRouterTest-asasas", "false", embeddedKafka);
    var consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps,
        new StringDeserializer(), new StringDeserializer());
    var testConsumer = consumerFactory.createConsumer();
    embeddedKafka.consumeFromEmbeddedTopics(testConsumer, topics);

    var encoderOutputRecord = KafkaTestUtils.getSingleRecord(testConsumer, asn1CoderTopics.getEncoderOutput());
    var timCertExpirationRecord = KafkaTestUtils.getSingleRecord(testConsumer, jsonTopics.getTimCertExpiration());
    var timTmcFilteredRecord = KafkaTestUtils.getSingleRecord(testConsumer, jsonTopics.getTimTmcFiltered());
    var encoderInputRecord = KafkaTestUtils.getSingleRecord(testConsumer, asn1CoderTopics.getEncoderInput());

    assertEquals("", timCertExpirationRecord.value());
    assertEquals("", timTmcFilteredRecord.value());
    assertEquals("", encoderInputRecord.value());
  }

}
