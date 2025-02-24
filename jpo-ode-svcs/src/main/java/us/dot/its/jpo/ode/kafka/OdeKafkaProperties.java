package us.dot.its.jpo.ode.kafka;

import java.util.Set;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

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