package us.dot.its.jpo.ode.kafka;

import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

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
      log.debug("Topic {} is disabled. Skipping sending message.", producerRecord.topic());
    }
    return producerRecord;
  }

  @Override
  public void onAcknowledgement(RecordMetadata recordMetadata, Exception e) {

  }

  @Override
  public void close() {

  }

  @Override
  public void configure(Map<String, ?> map) {

  }
}
