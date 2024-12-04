package us.dot.its.jpo.ode.kafka.producer;

import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
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
    implements ProducerInterceptor<String, String> {

  private final Set<String> disabledTopics;

  public DisabledTopicsStringProducerInterceptor(OdeKafkaProperties odeKafkaProperties) {
    this.disabledTopics = odeKafkaProperties.getDisabledTopics();
  }

  @Override
  public ProducerRecord<String, String> onSend(ProducerRecord<String, String> producerRecord) {
    if (disabledTopics.contains(producerRecord.topic())) {
      throw new DisabledTopicException(producerRecord.topic());
    }
    return producerRecord;
  }

  @Override
  public void onAcknowledgement(RecordMetadata recordMetadata, Exception e) {
    log.debug("Acknowledged message with offset {} on partition {}", recordMetadata.offset(),
        recordMetadata.partition());
  }

  @Override
  public void close() {
    log.debug("Closing StringProducerInterceptor");
  }

  @Override
  public void configure(Map<String, ?> map) {

  }
}
