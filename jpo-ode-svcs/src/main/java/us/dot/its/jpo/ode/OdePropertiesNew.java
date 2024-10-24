package us.dot.its.jpo.ode;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.udp.generic.GenericReceiverProperties;

@Configuration
@ConfigurationProperties(prefix = "ode")
public class OdePropertiesNew {
    private OdeKafkaProperties kafkaProperties;
    private GenericReceiverProperties receiverProperties;

    @Bean
    public OdeKafkaProperties kafkaProperties() {
        return new OdeKafkaProperties();
    }

    @Bean
    public GenericReceiverProperties receiverProperties() {
        return new GenericReceiverProperties();
    }
}
