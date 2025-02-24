package us.dot.its.jpo.ode.kafka;

import java.util.Set;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * A configuration properties class designed to manage Kafka-related configuration
 * settings within the application. This class utilizes Spring Boot's configuration
 * properties mechanism, allowing property values to be easily injected and
 * externalized.
 *
 * <p>The class provides configurable fields related to Kafka brokers, producer
 * settings, topic management, and Confluent-specific properties for enhanced
 * integration. Validation is applied via {@code OdeKafkaPropertiesValidator} to
 * ensure compliance with expected values for certain fields.
 *
 * <p>This class also provides default values for various producer properties,
 * including batch size, buffer memory, retries, and serializers, in order to simplify
 * Kafka producer configuration.
 *
 * <p>Nested classes such as {@code Producer} encapsulate additional Kafka-specific
 * configuration options, offering a structured way to define and validate such settings.
 *
 * <p>Key property groups include:
 * <ul>
 *   <li>{@code brokers}: Defines Kafka broker addresses in a host:port format.</li>
 *   <li>{@code disabledTopics}: A configurable set of topics that are excluded from certain application processes.</li>
 *   <li>{@code producer}: Configuration properties specific to Kafka producers, including serializers and batching settings.</li>
 *   <li>{@code kafkaType}: Indicates the type of Kafka setup (e.g., Confluent or standard).</li>
 *   <li>{@code confluent}: Holds properties required for connecting to a Confluent-managed Kafka cluster.</li>
 * </ul>
 *
 * <p>Validation rules include:
 * <ul>
 *   <li>{@code brokers} must be non-empty and follow the host:port format.</li>
 *   <li>{@code kafkaType} must match one of the predefined valid types.</li>
 *   <li>When {@code kafkaType} is set to "CONFLUENT", the {@code confluent} properties must include
 *       a username and password.</li>
 *   <li>{@code producer.acks} must contain one of the valid acknowledgment values.</li>
 * </ul>
 *
 */
@Configuration
@ConfigurationProperties(prefix = "ode.kafka")
@Data
@Validated(value = OdeKafkaPropertiesValidator.class)
public class OdeKafkaProperties {
  public static final String SERIALIZATION_STRING_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer";
  public static final String SERIALIZATION_BYTE_ARRAY_SERIALIZER = "org.apache.kafka.common.serialization.ByteArraySerializer";
  public static final int DEFAULT_PRODUCER_BUFFER_MEMORY_BYTES = 33554432;
  public static final int DEFAULT_PRODUCER_LINGER_MS = 1;
  public static final int DEFAULT_PRODUCER_BATCH_SIZE_BYTES = 16384;
  public static final int DEFAULT_PRODUCER_RETRIES = 0;
  public static final String DEFAULT_PRODUCER_ACKS = "all";
  public static final String COMPRESSION_TYPE = "zstd";
  private String brokers;
  private Set<String> disabledTopics;
  private Producer producer;
  private String kafkaType = "";
  private ConfluentProperties confluent;

  /**
   * The Producer class contains configuration properties for a Kafka producer. It includes
   * various parameters that control producer behavior and the serialization mechanisms
   * used for keys and values. This class provides default values for all its properties,
   * which can be overridden by values set in the application configuration file.
   */
  @Data
  public static class Producer {
    private Integer batchSize = DEFAULT_PRODUCER_BATCH_SIZE_BYTES;
    private Integer bufferMemory = DEFAULT_PRODUCER_BUFFER_MEMORY_BYTES;
    private Integer lingerMs = DEFAULT_PRODUCER_LINGER_MS;
    private Integer retries = DEFAULT_PRODUCER_RETRIES;
    private String acks = DEFAULT_PRODUCER_ACKS;
    private String keySerializer = SERIALIZATION_STRING_SERIALIZER;
    private String valueSerializer = SERIALIZATION_BYTE_ARRAY_SERIALIZER;
    private String compressionType = COMPRESSION_TYPE;
    private String partitionerClass = "org.apache.kafka.clients.producer.internals.DefaultPartitioner";
    private String type = "sync";
  }
}