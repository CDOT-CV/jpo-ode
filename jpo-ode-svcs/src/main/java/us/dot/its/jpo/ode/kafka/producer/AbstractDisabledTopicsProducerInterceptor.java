package us.dot.its.jpo.ode.kafka.producer;

import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

/**
 * Through a set of disabled topics provided during initialization, this interceptor enforces that
 * messages are not sent to these topics. If an attempt is made to send a message to a disabled
 * topic, the interceptor throws a DisabledTopicException, effectively preventing the message from
 * being dispatched.
 *
 * </p>The class also provides default logging behavior upon message acknowledgement and
 * during interceptor closure.
 *
 * @param <K> the type of the key for Kafka producer records
 * @param <V> the type of the value for Kafka producer records
 */
@Slf4j
public abstract class AbstractDisabledTopicsProducerInterceptor<K, V>
    implements ProducerInterceptor<K, V> {

  private final Set<String> disabledTopics;

  /**
   * Constructs an AbstractDisabledTopicsProducerInterceptor with a specified set of disabled
   * topics. This interceptor will prevent messages from being sent to any of these topics.
   *
   * @param disabledTopics a set of topic names that are disabled. Messages intended for these
   *                       topics will be intercepted and will not be sent.
   */
  protected AbstractDisabledTopicsProducerInterceptor(Set<String> disabledTopics) {
    this.disabledTopics = disabledTopics;
  }

  @Override
  public ProducerRecord<K, V> onSend(ProducerRecord<K, V> producerRecord) {
    if (disabledTopics.contains(producerRecord.topic())) {
      throw new DisabledTopicException(producerRecord.topic());
    }
    return producerRecord;
  }

  @Override
  public void onAcknowledgement(RecordMetadata recordMetadata, Exception e) {
    log.debug("Acknowledged message with offset {} on partition {}", recordMetadata.offset(),
        recordMetadata.partition());
    if (e != null) {
      log.error("Error acknowledging message", e);
    }
  }

  @Override
  public void close() {
    log.debug("Closing ProducerInterceptor");
  }

  @Override
  public void configure(Map<String, ?> configs) {
    // Default implementation
  }
}