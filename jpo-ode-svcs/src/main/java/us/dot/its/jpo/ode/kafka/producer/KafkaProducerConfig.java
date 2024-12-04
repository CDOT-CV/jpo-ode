package us.dot.its.jpo.ode.kafka.producer;

import java.util.Map;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.kafka.XMLOdeObjectSerializer;
import us.dot.its.jpo.ode.model.OdeObject;

/**
 * KafkaProducerConfig is a configuration class for setting up Kafka producers with Spring Boot.
 * This class utilizes the KafkaProperties and OdeKafkaProperties to define and construct the
 * necessary producer factories and Kafka templates for producing messages to Kafka topics.
 *
 * </p>It provides configuration for two types of Kafka producer factories and templates:
 * one for producing regular String messages and another for producing `OdeObject` messages
 * serialized as XML.
 *
 * </p>This configuration is crucial for integrating with Kafka by providing
 * necessary producer settings and managing producer instances.
 */
@EnableKafka
@Configuration
public class KafkaProducerConfig {

  private final KafkaProperties kafkaProperties;
  private final OdeKafkaProperties odeKafkaProperties;

  /**
   * Constructor for the KafkaProducerConfig class, which sets up the configuration for Kafka
   * producers using provided Kafka properties.
   *
   * @param kafkaProperties    the properties related to Kafka configuration as set up in the Spring
   *                           environment, providing necessary configurations for creating Kafka
   *                           producers.
   * @param odeKafkaProperties the properties specific to the ODE Kafka setup, including custom
   *                           configurations like Kafka type (e.g., CONFLUENT) and other
   *                           specialized settings for integrating with the ODE infrastructure.
   */
  public KafkaProducerConfig(KafkaProperties kafkaProperties,
      OdeKafkaProperties odeKafkaProperties) {
    this.kafkaProperties = kafkaProperties;
    this.odeKafkaProperties = odeKafkaProperties;
  }

  /**
   * Creates a Kafka ProducerFactory configured for producing messages with String keys
   * and String values. This factory sets up and manages the configuration needed for
   * producing messages to Kafka topics using the properties defined in the application
   * configuration.
   *
   * @return a ProducerFactory instance for creating Kafka producers with String key
   *         and value serializers. This includes any custom properties defined for
   *         Kafka producers, as well as additional settings for Confluent-based
   *         Kafka setups if applicable.
   */
  @Bean
  public ProducerFactory<String, String> producerFactory() {
    return new DefaultKafkaProducerFactory<>(buildProducerProperties());
  }

  /**
   * Creates and returns a KafkaTemplate that allows for sending messages with String keys
   * and String values to Kafka topics. This template is configured using the ProducerFactory
   * instance provided by the producerFactory() method, ensuring that it is set up with the
   * necessary serializers and additional properties defined in the application configuration.
   *
   * @return a KafkaTemplate instance configured for publishing messages to Kafka topics
   *         with String keys and values, facilitating message sending operations in Kafka.
   */
  @Bean
  public KafkaTemplate<String, String> kafkaTemplate(
      ProducerFactory<String, String> producerFactory,
      DisabledTopicsStringProducerInterceptor disabledTopicsStringProducerInterceptor) {
    var template = new KafkaTemplate<>(producerFactory);

    template.setProducerInterceptor(disabledTopicsStringProducerInterceptor);
    template.setProducerListener(new LoggingProducerListener<>());

    return template;
  }

  /**
   * Provides a bean of type DisabledTopicsStringProducerInterceptor which is used to intercept
   * Kafka producer records and prevent messages from being sent to disabled topics specified in the
   * OdeKafkaProperties configuration.
   *
   * @param odeKafkaProperties the ODE-specific Kafka properties which include the set of topics
   *                           that are disabled; the interceptor uses this configuration to
   *                           determine which topics are not permitted to receive messages.
   * @return an instance of DisabledTopicsStringProducerInterceptor configured with the disabled
   *         topics defined in the provided OdeKafkaProperties.
   */
  @Bean
  public DisabledTopicsStringProducerInterceptor disabledTopicsStringInterceptor(
      OdeKafkaProperties odeKafkaProperties) {
    return new DisabledTopicsStringProducerInterceptor(odeKafkaProperties);
  }

  /**
   * Creates a Kafka ProducerFactory specifically configured for handling messages with
   * String keys and OdeObject values. This factory utilizes a custom XML serializer
   * for OdeObjects, enabling proper serialization for Kafka message transmission.
   *
   * @return a ProducerFactory instance configured with String serializers for keys and
   *         a custom XMLOdeObjectSerializer for OdeObject values, using producer properties
   *         tailored to the application's Kafka and ODE settings.
   */
  @Bean
  public ProducerFactory<String, OdeObject> odeDataProducerFactory() {
    return new DefaultKafkaProducerFactory<>(buildProducerProperties(),
        new StringSerializer(), new XMLOdeObjectSerializer());
  }

  /**
   * Creates and returns a KafkaTemplate for sending messages with String keys
   * and OdeObject values to Kafka topics. The template is configured using the
   * ProducerFactory provided by the odeDataProducerFactory() method, ensuring
   * it incorporates the necessary serializers and properties for proper message
   * handling in the context of ODE and Kafka integration.
   *
   * @return a KafkaTemplate instance configured for handling messages with
   *         String keys and OdeObject values, enabling seamless message publication
   *         to Kafka topics within the application's messaging infrastructure.
   */
  @Bean
  public KafkaTemplate<String, OdeObject> odeDataKafkaTemplate(
      ProducerFactory<String, OdeObject> producerFactory,
      DisabledTopicsOdeObjectProducerInterceptor disabledTopicsOdeObjectProducerInterceptor
  ) {
    var template = new KafkaTemplate<>(producerFactory);
    template.setProducerInterceptor(disabledTopicsOdeObjectProducerInterceptor);
    template.setProducerListener(new LoggingProducerListener<>());

    return template;
  }

  private Map<String, Object> buildProducerProperties() {
    var producerProps = kafkaProperties.buildProducerProperties();
    if ("CONFLUENT".equals(this.odeKafkaProperties.getKafkaType())) {
      producerProps.putAll(this.odeKafkaProperties.getConfluent().buildConfluentProperties());
    }
    return producerProps;
  }

  /**
   * Provides a bean of type DisabledTopicsOdeObjectProducerInterceptor used
   * to intercept Kafka producer records, preventing messages from being sent
   * to disabled topics as specified in the OdeKafkaProperties configuration.
   *
   * @param odeKafkaProperties the ODE-specific Kafka properties which include
   *                           a set of disabled topics; the interceptor uses
   *                           this configuration to determine which topics are
   *                           not permitted to receive messages.
   * @return an instance of DisabledTopicsOdeObjectProducerInterceptor configured
   *         with the disabled topics defined in the provided OdeKafkaProperties.
   */
  @Bean
  public DisabledTopicsOdeObjectProducerInterceptor disabledTopicsOdeObjectInterceptor(
      OdeKafkaProperties odeKafkaProperties) {
    return new DisabledTopicsOdeObjectProducerInterceptor(odeKafkaProperties.getDisabledTopics());
  }
}
