package us.dot.its.jpo.ode.kafka;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
@ConfigurationProperties(prefix = "ode.kafka")
@Data
public class OdeKafkaProperties {
    private String brokers;
    private String type; // The type of Kafka broker to connect to. If set to "CONFLUENT", the broker will be Confluent Cloud. Otherwise, it will be a local Kafka broker.
    private Set<String> disabledTopics;
    private Producer producer;

    @Data
    public static class Producer {
        private String acks = "all";
        private String batchSize = "16384";
        private String bufferMemory = "33554432";
        private String keySerializer = "org.apache.kafka.common.serialization.StringSerializer";
        private String lingerMs = "1";
        private String partitionerClass;
        private String retries = "0";
        private String type = "sync";
        private String valueSerializer;
    }
}