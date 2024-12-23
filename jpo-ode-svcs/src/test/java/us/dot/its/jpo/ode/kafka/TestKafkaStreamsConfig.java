package us.dot.its.jpo.ode.kafka;

import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import us.dot.its.jpo.ode.OdeTimJsonTopology;
import us.dot.its.jpo.ode.test.utilities.EmbeddedKafkaHolder;

@TestConfiguration
public class TestKafkaStreamsConfig {

  @Bean
  public OdeTimJsonTopology odeTimJsonTopology(OdeKafkaProperties odeKafkaProperties,
      @Value("${ode.kafka.topics.json.tim}") String timTopic) {
    EmbeddedKafkaHolder.addTopics(timTopic);
    var topology = new OdeTimJsonTopology(odeKafkaProperties, timTopic);
    Awaitility.await().until(topology::isRunning);
    return topology;
  }
}
