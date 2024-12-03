package us.dot.its.jpo.ode.kafka;

import java.util.Map;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import us.dot.its.jpo.ode.model.OdeObject;

@EnableKafka
@Configuration
public class KafkaProducerConfig {

  private final KafkaProperties kafkaProperties;
  private final OdeKafkaProperties odeKafkaProperties;

  public KafkaProducerConfig(KafkaProperties kafkaProperties,
      OdeKafkaProperties odeKafkaProperties) {
    this.kafkaProperties = kafkaProperties;
    this.odeKafkaProperties = odeKafkaProperties;
  }

  @Bean
  public ProducerFactory<String, String> producerFactory() {
    return new DefaultKafkaProducerFactory<>(buildProducerProperties());
  }

  @Bean
  public KafkaTemplate<String, String> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }

  @Bean
  public ProducerFactory<String, OdeObject> odeDataProducerFactory() {
    return new DefaultKafkaProducerFactory<>(buildProducerProperties(),
        new StringSerializer(), new XMLOdeObjectSerializer());
  }

  private Map<String, Object> buildProducerProperties() {
    var producerProps = kafkaProperties.buildProducerProperties();
    if ("CONFLUENT".equals(this.odeKafkaProperties.getKafkaType())) {
      producerProps.putAll(this.odeKafkaProperties.getConfluent().buildConfluentProperties());
    }
    return producerProps;
  }

  @Bean
  public KafkaTemplate<String, OdeObject> odeDataKafkaTemplate() {
    return new KafkaTemplate<>(odeDataProducerFactory());
  }
}
