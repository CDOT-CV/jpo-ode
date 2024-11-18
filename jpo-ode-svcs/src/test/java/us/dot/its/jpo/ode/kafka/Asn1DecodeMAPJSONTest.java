package us.dot.its.jpo.ode.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import us.dot.its.jpo.ode.testUtilities.ApprovalTestCase;
import us.dot.its.jpo.ode.testUtilities.EmbeddedKafkaHolder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static us.dot.its.jpo.ode.testUtilities.ApprovalTestCase.deserializeTestCases;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@DirtiesContext
class Asn1DecodeMAPJSONTest {

    static final String INPUT_TOPIC = "topic.OdeRawEncodedMAPJson";
    static final String OUTPUT_TOPIC = "topic.Asn1DecoderInput";

    private static final EmbeddedKafkaBroker embeddedKafka = EmbeddedKafkaHolder.getEmbeddedKafka();

    static {
        EmbeddedKafkaHolder.getEmbeddedKafka().addTopics(INPUT_TOPIC, OUTPUT_TOPIC);
    }

    @Test
    void testProcess_ApprovalTest() throws IOException {
        String path = "src/test/resources/us.dot.its.jpo.ode.udp.map/JSONEncodedMAP_to_Asn1DecoderInput_Validation.json";
        List<ApprovalTestCase> approvalTestCases = deserializeTestCases(path);

        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka);
        DefaultKafkaProducerFactory<Integer, String> producerFactory = new DefaultKafkaProducerFactory<>(producerProps);
        Producer<Integer, String> producer = producerFactory.createProducer();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testT", "false", embeddedKafka);
        DefaultKafkaConsumerFactory<Integer, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<Integer, String> testConsumer = cf.createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(testConsumer, OUTPUT_TOPIC);

        for (ApprovalTestCase approvalTestCase : approvalTestCases) {
            // produce the test case input to the topic for consumption by the asn1RawMAPJSONConsumer
            ProducerRecord<Integer, String> r = new ProducerRecord<>(INPUT_TOPIC, approvalTestCase.getInput());
            producer.send(r);

            ConsumerRecord<Integer, String> actualRecord = KafkaTestUtils.getSingleRecord(testConsumer, OUTPUT_TOPIC);
            assertEquals(approvalTestCase.getExpected(), actualRecord.value(), approvalTestCase.getDescription());
        }
    }
}
