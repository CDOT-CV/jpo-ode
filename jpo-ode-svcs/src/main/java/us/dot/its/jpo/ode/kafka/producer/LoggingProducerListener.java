package us.dot.its.jpo.ode.kafka.producer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.support.ProducerListener;
import org.springframework.lang.Nullable;

@Slf4j
public class LoggingProducerListener<K, V>
    implements ProducerListener<K, V> {

  @Override
  public void onSuccess(ProducerRecord<K, V> producerRecord, RecordMetadata recordMetadata) {
    log.debug("Successfully produced key {} and value {} to topic {}", producerRecord.key(),
        producerRecord.value(), producerRecord.topic());
  }

  @Override
  public void onError(ProducerRecord<K, V> producerRecord, @Nullable RecordMetadata recordMetadata,
      Exception exception) {
    log.error("Error producing key {} and value {} to topic {}", producerRecord.key(),
        producerRecord.value(), producerRecord.topic(), exception);
  }
}
