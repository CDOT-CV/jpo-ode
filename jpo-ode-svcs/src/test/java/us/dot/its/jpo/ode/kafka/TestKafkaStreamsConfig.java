package us.dot.its.jpo.ode.kafka;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.FileSystemUtils;
import us.dot.its.jpo.ode.OdeTimJsonTopology;
import us.dot.its.jpo.ode.test.utilities.EmbeddedKafkaHolder;

/**
 * TestKafkaStreamsConfig is a test configuration class that provides a Kafka
 * Streams topology for testing purposes. It utilizes an embedded Kafka broker
 * to facilitate the testing of TIM (Traveler Information Message) JSON data
 * processing.
 */
@TestConfiguration
public class TestKafkaStreamsConfig {

  /**
   * Creates and initializes an instance of OdeTimJsonTopology for processing TIM
   * (Traveler Information Message) JSON data. This method adds the specified
   * Kafka topic to the embedded Kafka broker, creates the topology, and ensures
   * it is in a running state before returning.
   *
   * @param odeKafkaProperties the configuration properties for Kafka
   * @param timTopic           the name of the Kafka topic used for consuming TIM
   *                           JSON data.
   *
   * @return the initialized instance of OdeTimJsonTopology.
   */
  @Bean
  public OdeTimJsonTopology odeTimJsonTopology(
      @Value("${ode.kafka.topics.json.tim}") String timTopic,
      @Value("${ode.kafka.topics.json.tim-ktable}") String timKTableTopic,
      OdeKafkaProperties odeKafkaProperties,
      KafkaTemplate<String, String> kafkaTemplate) {
    EmbeddedKafkaHolder.addTopics(timTopic);
    Path stateDir;
    try {
      stateDir = Files.createTempDirectory("ode-tim-json-topology-test");
    } catch (IOException e) {
      throw new IllegalStateException("Unable to create Kafka Streams state directory", e);
    }
    // Best-effort cleanup so repeated test runs don't accumulate state directories
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      try {
        FileSystemUtils.deleteRecursively(stateDir);
      } catch (IOException ignored) {
        // files may still be locked by Kafka Streams; the OS temp cleaner will handle it
      }
    }));
    var topology = new OdeTimJsonTopology(odeKafkaProperties, timTopic, timKTableTopic,
        kafkaTemplate, stateDir.toString());
    Awaitility.await().until(topology::isRunning);
    return topology;
  }
}
