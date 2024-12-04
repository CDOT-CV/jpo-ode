package us.dot.its.jpo.ode.kafka.producer;

import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

@Slf4j
// Abstract base class for ProducerInterceptor with common functionality
public abstract class AbstractDisabledTopicsProducerInterceptor<K, V>
    implements ProducerInterceptor<K, V> {

  private final Set<String> disabledTopics;

  public AbstractDisabledTopicsProducerInterceptor(Set<String> disabledTopics) {
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