package us.dot.its.jpo.ode.services.asn1.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ContextConfiguration;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.kafka.producer.KafkaProducerConfig;
import us.dot.its.jpo.ode.kafka.topics.Asn1CoderTopics;
import us.dot.its.jpo.ode.kafka.topics.RawEncodedJsonTopics;
import us.dot.its.jpo.ode.test.utilities.EmbeddedKafkaHolder;

@SpringBootTest(
    classes = {
        KafkaProducerConfig.class,
        KafkaProperties.class
    },
    properties = {
        "ode.kafka.topics.raw-encoded-json.psm=topic.Asn1DecoderTestPSMJSON",
        "ode.kafka.topics.asn1.decoder-input=topic.Asn1DecoderPSMInput"
    })
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(value = {
    OdeKafkaProperties.class,
    Asn1CoderTopics.class,
    RawEncodedJsonTopics.class})
class Asn1DecodePSMJSONTest {

  @Autowired
  OdeKafkaProperties odeKafkaProperties;

  @Autowired
  Asn1CoderTopics asn1CoderTopics;
  @Autowired
  RawEncodedJsonTopics rawEncodedJsonTopics;
  @Autowired
  private KafkaTemplate<String, String> kafkaTemplate;

  @Test
  void testProcess() throws JSONException, IOException {
    var embeddedKafka = EmbeddedKafkaHolder.getEmbeddedKafka();
    EmbeddedKafkaHolder.addTopics(asn1CoderTopics.getDecoderInput(), rawEncodedJsonTopics.getPsm());
    Asn1DecodePSMJSON testDecodePsmJson =
        new Asn1DecodePSMJSON(odeKafkaProperties, asn1CoderTopics.getDecoderInput());

    Map<String, Object> consumerProps =
        KafkaTestUtils.consumerProps("Asn1DecodePSMJSONTest", "false", embeddedKafka);
    var cf =
        new DefaultKafkaConsumerFactory<>(consumerProps,
            new StringDeserializer(), new StringDeserializer());
    var testConsumer = cf.createConsumer();
    embeddedKafka.consumeFromAnEmbeddedTopic(testConsumer, asn1CoderTopics.getDecoderInput());

    var classLoader = getClass().getClassLoader();
    InputStream inputStream = classLoader
        .getResourceAsStream("us/dot/its/jpo/ode/services/asn1/messages/decoder-input-psm.json");
    assert inputStream != null;
    var psmJson = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

    testDecodePsmJson.process(psmJson);

    inputStream = classLoader
        .getResourceAsStream("us/dot/its/jpo/ode/services/asn1/messages/expected-psm.xml");
    assert inputStream != null;
    var expectedPsm = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

    var produced =
        KafkaTestUtils.getSingleRecord(testConsumer, asn1CoderTopics.getDecoderInput());
    var odePsmData = produced.value();
    assertEquals(expectedPsm, odePsmData);
  }
}
