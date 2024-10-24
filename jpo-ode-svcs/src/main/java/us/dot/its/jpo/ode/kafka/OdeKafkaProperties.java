package us.dot.its.jpo.ode.kafka;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
@ConfigurationProperties(prefix = "ode.kafka")
@Data
public class OdeKafkaProperties {
    @Value("${ode.kafka.brokers:localhost:9092}")
    private String brokers;
    @Value("${ode.kafka.producer-type:sync}")
    private String producerType;
    @Value("${ode.kafka.disabled-topics:}")
    private Set<String> disabledTopics;
}