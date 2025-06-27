package us.dot.its.jpo.ode.kafka.topics;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for JSON-formatted Kafka topics.
 */
@Configuration
@ConfigurationProperties(prefix = "ode.kafka.topics.json")
@Data
public class JsonTopics {
  private String bsm;
  private String map;
  private String psm;
  private String spat;
  private String srm;
  private String ssm;
  private String tim;
  private String sdsm;

  private String driverAlert;

  private String bsmFiltered;
  private String spatFiltered;
  private String timFiltered;
  private String timTmcFiltered;

  private String timCertExpiration;
}
