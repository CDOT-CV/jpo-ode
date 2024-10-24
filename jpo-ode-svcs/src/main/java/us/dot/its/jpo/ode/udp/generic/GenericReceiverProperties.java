package us.dot.its.jpo.ode.udp.generic;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import us.dot.its.jpo.ode.kafka.RawEncodedJsonTopics;

@Configuration
@Data
@ConfigurationProperties(prefix = "ode.receivers.generic")
public class GenericReceiverProperties {
    private int receiverPort;
    private int bufferSize;

    private RawEncodedJsonTopics rawEncodedJsonTopics;

    @Bean
    public RawEncodedJsonTopics rawEncodedJsonTopics() {
        return new RawEncodedJsonTopics();
    }
}
