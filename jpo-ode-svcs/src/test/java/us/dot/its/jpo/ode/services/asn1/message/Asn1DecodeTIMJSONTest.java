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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import us.dot.its.jpo.ode.kafka.KafkaConsumerConfig;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.kafka.producer.KafkaProducerConfig;
import us.dot.its.jpo.ode.kafka.topics.Asn1CoderTopics;
import us.dot.its.jpo.ode.kafka.topics.RawEncodedJsonTopics;
import us.dot.its.jpo.ode.test.utilities.EmbeddedKafkaHolder;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties;


@SpringBootTest(
    classes = {
        KafkaProducerConfig.class,
        KafkaConsumerConfig.class
    },
    properties = {
        "ode.kafka.topics.raw-encoded-json.tim=topic.Asn1DecoderTestTIMJSON",
        "ode.kafka.topics.asn1.decoder-input=topic.Asn1DecoderTIMInput"
    })
@EnableConfigurationProperties
@ContextConfiguration(classes = {
    UDPReceiverProperties.class, OdeKafkaProperties.class,
    RawEncodedJsonTopics.class, KafkaProperties.class, Asn1CoderTopics.class
})
@DirtiesContext
class Asn1DecodeTIMJSONTest {

  @Autowired
  OdeKafkaProperties odeKafkaProperties;

  @Autowired
  Asn1CoderTopics asn1CoderTopics;
  @Autowired
  RawEncodedJsonTopics rawEncodedJsonTopics;

  @Test
  void testProcess() throws JSONException, IOException {
    var embeddedKafka = EmbeddedKafkaHolder.getEmbeddedKafka();
    EmbeddedKafkaHolder.addTopics(asn1CoderTopics.getDecoderInput(), rawEncodedJsonTopics.getTim());

    Map<String, Object> consumerProps =
        KafkaTestUtils.consumerProps("Asn1DecodeTIMJSONTestConsumer", "false", embeddedKafka);
    var cf =
        new DefaultKafkaConsumerFactory<>(consumerProps,
            new StringDeserializer(), new StringDeserializer());
    Consumer<String, String> testConsumer = cf.createConsumer();
    embeddedKafka.consumeFromAnEmbeddedTopic(testConsumer, asn1CoderTopics.getDecoderInput());

    Asn1DecodeTIMJSON testDecodeTimJson =
        new Asn1DecodeTIMJSON(odeKafkaProperties, asn1CoderTopics.getDecoderInput());

    var classLoader = getClass().getClassLoader();
    InputStream inputStream = classLoader
        .getResourceAsStream("us/dot/its/jpo/ode/services/asn1/messages/decoder-input-tim.json");
    assert inputStream != null;
    var json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    testDecodeTimJson.process(json);

    inputStream = classLoader
        .getResourceAsStream("us/dot/its/jpo/ode/services/asn1/messages/expected-tim.xml");
    assert inputStream != null;
    var expectedTim = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

    var produced =
        KafkaTestUtils.getSingleRecord(testConsumer, asn1CoderTopics.getDecoderInput());
    assertEquals(expectedTim, produced.value());
  }
}
