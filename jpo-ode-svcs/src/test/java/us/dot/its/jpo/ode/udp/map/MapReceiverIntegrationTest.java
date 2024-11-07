package us.dot.its.jpo.ode.udp.map;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertTrue;


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
    private static EmbeddedKafkaRule embeddedKafka = new EmbeddedKafkaRule(1,
            false,
            1,
            "topic.OdeRawEncodedMAPJson").kafkaPorts(9092);

    ServiceManager rm;
    TestUDPClient udpClient;
    MapReceiver mapReceiver;

    // Set up a MapReceiver
    // Start the MapReceiver in a new thread
    // Wait for the MapReceiver to start
    // Send a UDP packet to the MapReceiver
    // Wait for the MapReceiver to process the packet
    // Verify that the MapReceiver produced the expected output on the expected topic
    // Stop the MapReceiver
    // Wait for the MapReceiver to stop
    // Verify that the MapReceiver stopped
    @BeforeEach
    public void setUp() {
        rm = new ServiceManager(new UdpServiceThreadFactory("UdpReceiverManager"));
        embeddedKafka.before();
        mapReceiver = new MapReceiver(udpReceiverProperties.getMap(),
                odeKafkaProperties,
                rawEncodedJsonTopics.getMap());

    }

    @AfterEach
    public void tearDown() {
        mapReceiver.setStopped(true);
        udpClient.close();
    }

    static class TestRow {
        String messageType;
        String timestamp;
        String payload;
    }

    // Test that the MapReceiver can receive a UDP packet and publish the expected output on the expected topic
    @Test
    void testMapReceiver() throws IOException {
        // Read from MAP_Validation.csv into a List of TestRow objects
        List<TestRow> rows = new ArrayList<>();
        // Read from jpo-ode-svcs/src/test/resources/us.dot.its.jpo.ode.udp.map/MAP_Validation.csv into a List of TestRow objects
        String path = "src/test/resources/us.dot.its.jpo.ode.udp.map/MAP_Validation.csv";
        File file = new File(path);

        Scanner scanner = new Scanner(file);
        // skip the header
        scanner.nextLine();

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] parts = line.split("\\|");
            TestRow row = new TestRow();
            row.messageType = parts[0];
            row.timestamp = parts[1];
            row.payload = "\u0000\u0012" + parts[2]; // prepend the 2-byte length hex code start flag to the payload
            rows.add(row);
        }


        // Start the MapReceiver in a new thread
        rm.submit(mapReceiver);

        udpClient = new TestUDPClient(udpReceiverProperties.getMap().getReceiverPort());

        udpClient.send(rows.getFirst().payload);

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("testT", "false", embeddedKafka.getEmbeddedKafka());
        DefaultKafkaConsumerFactory<Integer, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        Consumer<Integer, String> consumer = cf.createConsumer();
        embeddedKafka.getEmbeddedKafka().consumeFromAllEmbeddedTopics(consumer);

        ConsumerRecords<Integer, String> replies = KafkaTestUtils.getRecords(consumer);
        assertTrue(replies.count() > 0);

        // Wait for the MapReceiver to process the packet
        // Verify that the MapReceiver produced the expected output on the expected topic
        // Stop the MapReceiver
        // Wait for the MapReceiver to stop
        // Verify that the MapReceiver stopped

    }

}