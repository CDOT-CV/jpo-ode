package us.dot.its.jpo.ode.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

/**
 * Verifies that {@link SerializationConfig} cooperates with Spring Boot's Jackson
 * auto-configuration, as it does when the full application starts.
 *
 * <p>Most {@code @SpringBootTest}s in this module list their configuration classes explicitly and
 * therefore never load {@link JacksonAutoConfiguration}. This test loads it on purpose, because
 * {@code JacksonAutoConfiguration.jacksonJsonMapper} is a {@code @Primary} bean that only backs off
 * when a bean of type {@code JsonMapper} already exists.
 */
class SerializationConfigTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
      .withUserConfiguration(SerializationConfig.class, ObjectMapperConsumerConfig.class);

  @Test
  void contextStartsWhenObjectMapperIsInjectedAlongsideJacksonAutoConfiguration() {
    contextRunner.run(context -> assertThat(context).hasNotFailed());
  }

  @Test
  void injectedObjectMapperIsTheOneFromSerializationConfig() {
    contextRunner.run(context -> {
      assertThat(context).hasNotFailed();
      ObjectMapper injected = context.getBean(ObjectMapperConsumer.class).objectMapper();
      assertThat(injected).isSameAs(context.getBean("objectMapper"));
    });
  }

  /**
   * Stand-in for production beans such as {@code KafkaProducerConfig.kafkaTemplate},
   * {@code WebClientConfig.restTemplate}, {@code Asn1EncodedDataRouter} and
   * {@code RawEncodedJsonService}, which inject an unqualified {@link ObjectMapper}.
   */
  record ObjectMapperConsumer(ObjectMapper objectMapper) {
  }

  @Configuration(proxyBeanMethods = false)
  static class ObjectMapperConsumerConfig {

    @Bean
    ObjectMapperConsumer objectMapperConsumer(ObjectMapper objectMapper) {
      return new ObjectMapperConsumer(objectMapper);
    }
  }
}
