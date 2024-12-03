package us.dot.its.jpo.ode.kafka;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ode.kafka.confluent")
@Data
public class ConfluentProperties {
    private String username;
    private String password;

    public Map<String, Object> buildConfluentProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("ssl.endpoint.identification.algorithm", "https");
        props.put("security.protocol", "SASL_SSL");
        props.put("sasl.mechanism", "PLAIN");
        props.put("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required " +
            "username=\"" + username + "\" " +
            "password=\"" + password + "\";");
        return props;
    }
}
