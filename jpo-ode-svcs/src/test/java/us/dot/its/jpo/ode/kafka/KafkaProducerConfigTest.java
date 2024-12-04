package us.dot.its.jpo.ode.kafka;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import us.dot.its.jpo.ode.kafka.DisabledTopicsStringProducerInterceptor.DisabledTopicException;
import us.dot.its.jpo.ode.model.OdeObject;
import us.dot.its.jpo.ode.test.utilities.EmbeddedKafkaHolder;

@Slf4j
@SpringBootTest(
    classes = {
        KafkaProducerConfig.class,
        OdeKafkaProperties.class,
        KafkaProperties.class
    }
)
@EnableConfigurationProperties
class KafkaProducerConfigTest {

  @Autowired
  KafkaProducerConfig kafkaProducerConfig;
  @Autowired
  DisabledTopicsStringProducerInterceptor interceptor;
  @Autowired
  OdeKafkaProperties odeKafkaProperties;

  @Test
  void odeDataProducerFactory_shouldReturnNonNull() {
    ProducerFactory<String, OdeObject> producerFactory =
        kafkaProducerConfig.odeDataProducerFactory();
    assertNotNull(producerFactory);
  }

  @Test
  void odeDataProducerFactory_shouldReturnDefaultKafkaProducerFactory() {
    ProducerFactory<String, OdeObject> producerFactory =
        kafkaProducerConfig.odeDataProducerFactory();
    assertNotNull(producerFactory);
    assertInstanceOf(DefaultKafkaProducerFactory.class, producerFactory);
  }

  @Test
  void kafkaTemplateInterceptorPreventsSendingToDisabledTopics() {
    var embeddedKafka = EmbeddedKafkaHolder.getEmbeddedKafka();
    try {
      for (String topic : odeKafkaProperties.getDisabledTopics()) {
        embeddedKafka.addTopics(new NewTopic(topic, 1, (short) 1));
      }
    } catch (Exception e) {
      // ignore because we only care that the topics exist
    }

    var consumerProps =
        KafkaTestUtils.consumerProps(this.getClass().getSimpleName(), "false", embeddedKafka);
    var cf = new DefaultKafkaConsumerFactory<String, String>(consumerProps);
    var consumer = cf.createConsumer();
    embeddedKafka.consumeFromAllEmbeddedTopics(consumer);

    KafkaTemplate<String, String> kafkaTemplate =
        kafkaProducerConfig.kafkaTemplate(kafkaProducerConfig.producerFactory(), interceptor);

    // Attempting to send to a disabled topic
    for (String topic : odeKafkaProperties.getDisabledTopics()) {
      assertThrows(DisabledTopicException.class, () -> kafkaTemplate.send(topic, "key", "value"));

      var records = KafkaTestUtils.getEndOffsets(consumer, topic, 0);
      // Assert that the message to the disabled topic was intercepted and not sent
      assertTrue(records.entrySet().stream().allMatch(e -> e.getValue() == 0L));
    }
  }

  @Test
  void kafkaTemplateInterceptorAllowsSendingToTopicsNotInDisabledSet() {
    var embeddedKafka = EmbeddedKafkaHolder.getEmbeddedKafka();
    for (String topic : odeKafkaProperties.getDisabledTopics()) {
      var stringExceptionMap =
          embeddedKafka.addTopicsWithResults(new NewTopic(topic, 1, (short) 1));
      log.debug("Created topic {} with result {}", topic, stringExceptionMap);
    }
    String enabledTopic = "topic.enabled" + this.getClass().getSimpleName();
    var stringExceptionMap =
        embeddedKafka.addTopicsWithResults(new NewTopic(enabledTopic, 1, (short) 1));
    log.debug("Created topic {} with result {}", enabledTopic, stringExceptionMap);

    var consumerProps =
        KafkaTestUtils.consumerProps(this.getClass().getSimpleName(), "false", embeddedKafka);
    var cf = new DefaultKafkaConsumerFactory<String, String>(consumerProps);
    var consumer = cf.createConsumer();
    embeddedKafka.consumeFromAllEmbeddedTopics(consumer);

    KafkaTemplate<String, String> kafkaTemplate =
        kafkaProducerConfig.kafkaTemplate(kafkaProducerConfig.producerFactory(), interceptor);

    // Attempting to send to a topic not in the disabledTopics set
    var completableFuture = kafkaTemplate.send(enabledTopic, "key", "value");
    Awaitility.await().until(completableFuture::isDone);

    var records = KafkaTestUtils.getEndOffsets(consumer, enabledTopic, 0);
    assertTrue(records.entrySet().stream().allMatch(e -> e.getValue() > 0L));
  }
}