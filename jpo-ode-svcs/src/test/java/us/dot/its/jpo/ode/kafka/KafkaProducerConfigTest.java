package us.dot.its.jpo.ode.kafka;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties.Producer;
import us.dot.its.jpo.ode.kafka.producer.DisabledTopicException;
import us.dot.its.jpo.ode.kafka.producer.KafkaProducerConfig;
import us.dot.its.jpo.ode.model.OdeObject;
import us.dot.its.jpo.ode.test.utilities.EmbeddedKafkaHolder;

@Slf4j
@SpringBootTest(
    classes = {
        KafkaProducerConfig.class,
        KafkaProducerConfigTest.OdeKafkaPropertiesTestConfig.class,
        KafkaProperties.class
    }
)
@EnableConfigurationProperties
@DirtiesContext
class KafkaProducerConfigTest {

  @Autowired
  KafkaProducerConfig kafkaProducerConfig;
  @Autowired
  OdeKafkaProperties odeKafkaProperties;

  EmbeddedKafkaBroker embeddedKafka;
  KafkaTemplate<String, String> stringKafkaTemplate;
  KafkaTemplate<String, OdeObject> odeObjectKafkaTemplate;

  @BeforeEach
  public void beforeClass() {
    embeddedKafka = EmbeddedKafkaHolder.getEmbeddedKafka();
    EmbeddedKafkaHolder.addTopics(odeKafkaProperties.getDisabledTopics().toArray(new String[0]));
    stringKafkaTemplate =
        kafkaProducerConfig.kafkaTemplate(kafkaProducerConfig.producerFactory());
    odeObjectKafkaTemplate =
        kafkaProducerConfig.odeDataKafkaTemplate(kafkaProducerConfig.odeDataProducerFactory());
  }

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
    var consumerProps =
        KafkaTestUtils.consumerProps("interceptor-disabled",
            "false",
            embeddedKafka);
    var cf = new DefaultKafkaConsumerFactory<>(consumerProps,
        new StringDeserializer(), new StringDeserializer());
    var consumer = cf.createConsumer();
    embeddedKafka.consumeFromEmbeddedTopics(consumer,
        odeKafkaProperties.getDisabledTopics().toArray(new String[0]));

    // Attempting to send to a disabled topic
    for (String topic : odeKafkaProperties.getDisabledTopics()) {
      assertThrows(DisabledTopicException.class,
          () -> stringKafkaTemplate.send(topic, "key", "value"));

      var records = KafkaTestUtils.getEndOffsets(consumer, topic, 0);
      // Assert that the message we attempted to send to the disabled topic was intercepted
      // and not sent
      assertTrue(records
          .entrySet()
          .stream()
          .allMatch(e -> e.getValue() == 0L)
      );
    }
  }

  @Test
  void kafkaTemplateInterceptorAllowsSendingToTopicsNotInDisabledSet() {
    String enabledTopic = "topic.enabled" + this.getClass().getSimpleName();
    EmbeddedKafkaHolder.addTopics(enabledTopic);

    var consumerProps =
        KafkaTestUtils.consumerProps("interceptor-enabled", "false", embeddedKafka);
    var cf = new DefaultKafkaConsumerFactory<String, String>(consumerProps);
    var consumer = cf.createConsumer();
    embeddedKafka.consumeFromAnEmbeddedTopic(consumer, enabledTopic);

    // Attempting to send to a topic not in the disabledTopics set with the string template
    var stringCompletableFuture = stringKafkaTemplate.send(enabledTopic, "key", "value");
    Awaitility.await().until(stringCompletableFuture::isDone);

    var records = KafkaTestUtils.getEndOffsets(consumer, enabledTopic, 0);
    assertTrue(records.entrySet().stream().allMatch(e -> e.getValue() > 0L));
  }

  @TestConfiguration
  static class OdeKafkaPropertiesTestConfig {

    @Bean
    public OdeKafkaProperties odeKafkaProperties() {
      OdeKafkaProperties odeKafkaProperties = new OdeKafkaProperties();
      odeKafkaProperties.setBrokers("localhost:4242");
      odeKafkaProperties.setProducer(new Producer());
      var uniqueSuffix = UUID.randomUUID().toString().substring(0, 4);
      odeKafkaProperties.setDisabledTopics(Set.of(
          "topic.OdeBsmRxPojo" + uniqueSuffix,
          "topic.OdeBsmTxPojo" + uniqueSuffix,
          "topic.OdeBsmDuringEventPojo" + uniqueSuffix,
          "topic.OdeTimBroadcastPojo" + uniqueSuffix
      ));

      return odeKafkaProperties;
    }
  }
}