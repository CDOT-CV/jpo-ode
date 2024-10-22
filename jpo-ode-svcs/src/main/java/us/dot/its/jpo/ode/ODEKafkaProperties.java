package us.dot.its.jpo.ode;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import us.dot.its.jpo.ode.eventlog.EventLogger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Configuration
@ConfigurationProperties(prefix = "ode.kafka")
@Data
public class ODEKafkaProperties {

    private static final Logger logger = LoggerFactory.getLogger(ODEKafkaProperties.class);

    @Value("${ode.kafka.brokers:localhost:9092}")
    private String brokers;
    @Value("${ode.kafka.producer-type:sync}")
    private String producerType;
    @Value("${ode.kafka.disabled-topics:}")
    private List<String> disabledTopics;

    private String hostId;

    public String getHostId() {
        if (this.hostId == null || this.hostId.isEmpty()) {
            initializeHostId();
        }
        return hostId;
    }

    private void initializeHostId() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            // Let's just use a random hostname
            hostname = UUID.randomUUID().toString();
            logger.error("Unknown host error: {}, using random", e);
        }
        this.hostId = hostname;
        logger.info("Host ID: {}", hostId);
        EventLogger.logger.info("Initializing services on host {}", hostId);
    }

    public Set<String> getDisabledTopicsSet() {
        return Set.copyOf(disabledTopics);
    }
}