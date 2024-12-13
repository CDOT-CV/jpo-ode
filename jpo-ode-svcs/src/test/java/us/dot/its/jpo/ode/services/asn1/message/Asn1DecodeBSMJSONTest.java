package us.dot.its.jpo.ode.services.asn1.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.kafka.topics.Asn1CoderTopics;
import us.dot.its.jpo.ode.test.utilities.EmbeddedKafkaHolder;

@SuppressWarnings({"checkstyle:abbreviationAsWordInName", "checkstyle:linelength"})
@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(value = {OdeKafkaProperties.class, Asn1CoderTopics.class})
class Asn1DecodeBSMJSONTest {

  @Autowired
  OdeKafkaProperties odeKafkaProperties;

  @Autowired
  Asn1CoderTopics asn1CoderTopics;

  @Test
  void testProcess() throws JSONException, IOException {
    var embeddedKafka = EmbeddedKafkaHolder.getEmbeddedKafka();
    EmbeddedKafkaHolder.addTopics(asn1CoderTopics.getDecoderInput());

    Asn1DecodeBSMJSON testDecodeBsmJson =
        new Asn1DecodeBSMJSON(odeKafkaProperties, asn1CoderTopics.getDecoderInput());
    Map<String, Object> consumerProps =
        KafkaTestUtils.consumerProps("Asn1DecodeBSMJSONTestConsumer", "false", embeddedKafka);
    var cf =
        new DefaultKafkaConsumerFactory<>(consumerProps,
            new StringDeserializer(), new StringDeserializer());
    Consumer<String, String> testConsumer = cf.createConsumer();
    embeddedKafka.consumeFromAnEmbeddedTopic(testConsumer, asn1CoderTopics.getDecoderInput());

    var classLoader = getClass().getClassLoader();
    InputStream inputStream = classLoader
        .getResourceAsStream("us/dot/its/jpo/ode/services/asn1/messages/decoder-input-bsm.json");
    assert inputStream != null;
    String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    testDecodeBsmJson.process(json);

    inputStream = classLoader
        .getResourceAsStream("us/dot/its/jpo/ode/services/asn1/messages/expected-bsm.xml");
    assert inputStream != null;
    var expectedBsm = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

    var produced =
        KafkaTestUtils.getSingleRecord(testConsumer, asn1CoderTopics.getDecoderInput());
    var odeBsmData = produced.value();
    assertEquals(expectedBsm, odeBsmData);
  }
}
