package us.dot.its.jpo.ode.kafka.producer;

import lombok.extern.slf4j.Slf4j;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;

/**
 * The DisabledTopicsStringProducerInterceptor class implements the ProducerInterceptor interface
 * for Kafka producers that work with String keys and values. This interceptor checks whether the
 * Kafka topic specified in a ProducerRecord is disabled. If the topic is disabled, it throws a
 * DisabledTopicException, preventing the message from being sent.
 * See {@link AbstractDisabledTopicsProducerInterceptor} for more detail
 */
@Slf4j
public class DisabledTopicsStringProducerInterceptor
    extends AbstractDisabledTopicsProducerInterceptor<String, String> {

  @SuppressWarnings("checkstyle:missingJavadocMethod")
  public DisabledTopicsStringProducerInterceptor(OdeKafkaProperties odeKafkaProperties) {
    super(odeKafkaProperties.getDisabledTopics());
  }
}
