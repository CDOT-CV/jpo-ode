package us.dot.its.jpo.ode.kafka;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
@ConfigurationProperties(prefix = "ode.kafka", ignoreInvalidFields = true)
@Data
public class OdeKafkaProperties {
    private String brokers;
    private Set<String> disabledTopics;
    private Producer producer;

    @Data
    public static class Producer {
        private Integer batchSize = 16384;
        private Integer bufferMemory = 33554432;
        private Integer lingerMs = 1;
        private Integer retries = 0;
        private String acks = "all";
        private String keySerializer = "org.apache.kafka.common.serialization.StringSerializer";
        private String partitionerClass = "org.apache.kafka.clients.producer.internals.DefaultPartitioner";
        private String type = "sync";
        private String valueSerializer = "org.apache.kafka.common.serialization.ByteArraySerializer";
    }
}