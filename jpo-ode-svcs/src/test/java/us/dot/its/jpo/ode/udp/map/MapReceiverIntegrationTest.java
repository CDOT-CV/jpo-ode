package us.dot.its.jpo.ode.udp.map;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.ClassRule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.rule.EmbeddedKafkaRule;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.kafka.RawEncodedJsonTopics;
import us.dot.its.jpo.ode.udp.TestUDPClient;
import us.dot.its.jpo.ode.udp.controller.ServiceManager;
import us.dot.its.jpo.ode.udp.controller.UDPReceiverProperties;
import us.dot.its.jpo.ode.udp.controller.UdpServiceThreadFactory;
import us.dot.its.jpo.ode.util.DateTimeUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(value = {UDPReceiverProperties.class, OdeKafkaProperties.class, RawEncodedJsonTopics.class, KafkaProperties.class})
@RunWith(SpringRunner.class)
@DirtiesContext
@Slf4j
class MapReceiverIntegrationTest {

    @Autowired
    UDPReceiverProperties udpReceiverProperties;

    @Autowired
    OdeKafkaProperties odeKafkaProperties;

    @Autowired
    RawEncodedJsonTopics rawEncodedJsonTopics;

    @ClassRule
    private static EmbeddedKafkaRule embeddedKafka = new EmbeddedKafkaRule(1, false, 1)
            .kafkaPorts(9092);

    ServiceManager rm;
    TestUDPClient udpClient;
    MapReceiver mapReceiver;

    @BeforeEach
    public void setUp() {
        rm = new ServiceManager(new UdpServiceThreadFactory("UdpReceiverManager"));

        // Start the embedded Kafka broker and add the topics
        embeddedKafka.before();
        embeddedKafka.getEmbeddedKafka().addTopics(rawEncodedJsonTopics.getMap());

        mapReceiver = new MapReceiver(udpReceiverProperties.getMap(),
                odeKafkaProperties,
                rawEncodedJsonTopics.getMap());

        // Set the clock to a fixed time so that the MapReceiver will produce the same output every time
        DateTimeUtils.setClock(Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), Clock.systemUTC().getZone()));
    }

    @AfterEach
    public void tearDown() {
        mapReceiver.setStopped(true);
        udpClient.close();
    }

    @Getter
    @Setter
    public static class TestCases {
        public List<TestCase> cases = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class TestCase {
        public String description;
        public String input;
        public String expected;
    }

    // Test that the MapReceiver can receive a UDP packet and publish the expected output on the expected topic
    @Test
    void testMapReceiver() throws IOException {
        String path = "src/test/resources/us.dot.its.jpo.ode.udp.map/MAP_Validation.json";
        TestCases testCases = deserializeTestRows(path);

        // Start the MapReceiver in a new thread
        rm.submit(mapReceiver);

        // Set up a Kafka consumer
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testT", "false", embeddedKafka.getEmbeddedKafka());
        DefaultKafkaConsumerFactory<Integer, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<Integer, String> consumer = cf.createConsumer();
        embeddedKafka.getEmbeddedKafka().consumeFromAnEmbeddedTopic(consumer, rawEncodedJsonTopics.getMap());

        udpClient = new TestUDPClient(udpReceiverProperties.getMap().getReceiverPort());

        for (TestCase testCase : testCases.getCases()) {
            udpClient.send(testCase.getInput());
            ConsumerRecord<Integer, String> produced = KafkaTestUtils.getSingleRecord(consumer, rawEncodedJsonTopics.getMap());
            JSONObject producedJson = new JSONObject(produced.value());
            JSONObject expectedJson = new JSONObject(testCase.getExpected());
            assertEquals(expectedJson, producedJson);
        }
    }

    private TestCases deserializeTestRows(String path) throws IOException {
        File file = new File(path);
        byte[] jsonData = Files.readAllBytes(file.toPath());
        JSONObject jsonObject = new JSONObject(new String(jsonData));

        JSONArray jsonArray = jsonObject.getJSONArray("cases");

        TestCases testCases = new TestCases();
        for (int i = 0; i < jsonArray.length(); i++) {
            TestCase testCase = new TestCase();
            JSONObject json = jsonArray.getJSONObject(i);

            testCase.setDescription(json.getString("description"));

            JSONObject input = json.getJSONObject("input");
            testCase.setInput("\u0000\u0012" + input.toString()); // Add the 2-byte length prefix to the input

            JSONObject expected = json.getJSONObject("expected");
            testCase.setExpected(expected.toString());

            testCases.getCases().add(testCase);
        }
        return testCases;
    }
}