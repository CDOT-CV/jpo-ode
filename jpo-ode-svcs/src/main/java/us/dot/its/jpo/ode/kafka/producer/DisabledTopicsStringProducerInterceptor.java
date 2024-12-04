package us.dot.its.jpo.ode.kafka.producer;

import lombok.extern.slf4j.Slf4j;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;

/**
 * The DisabledTopicsStringProducerInterceptor class implements the ProducerInterceptor interface
 * for Kafka producers that work with String keys and values. This interceptor checks whether the
 * Kafka topic specified in a ProducerRecord is disabled. If the topic is disabled, it throws a
 * DisabledTopicException, preventing the message from being sent.
 *
 * </p>This interceptor is used to enforce topic-based controls within Kafka message
 * publishing, allowing certain topics to be disabled dynamically based on the configuration.
 */
@Slf4j
public class DisabledTopicsStringProducerInterceptor
    extends AbstractDisabledTopicsProducerInterceptor<String, String> {

  public DisabledTopicsStringProducerInterceptor(OdeKafkaProperties odeKafkaProperties) {
    super(odeKafkaProperties.getDisabledTopics());
  }
}
