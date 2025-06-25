package us.dot.its.jpo.ode.kafka.topics;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for ODE Kafka POJO topics.
 */
@Configuration
@ConfigurationProperties(prefix = "ode.kafka.topics.pojo")
@Data
public class PojoTopics {
  private String timBroadcast;
}
