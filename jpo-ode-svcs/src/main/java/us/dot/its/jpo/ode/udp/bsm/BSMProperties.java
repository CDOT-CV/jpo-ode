package us.dot.its.jpo.ode.udp.bsm;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ode.bsm")
public class BSMProperties {

    private int receiverPort;
    private int bufferSize;

    @Value("${ode.bsm.topics.pojo}")
    private String pojoTopic;
    @Value("${ode.bsm.topics.json}")
    private String jsonTopic;
    @Value("${ode.bsm.topics.rx-pojo}")
    private String rxPojoTopic;
    @Value("${ode.bsm.topics.tx-pojo}")
    private String txPojoTopic;
    @Value("${ode.bsm.topics.during-event-pojo}")
    private String duringEventPojoTopic;
    @Value("${ode.bsm.topics.filtered-json}")
    private String filteredJsonTopic;
    @Value("${ode.bsm.topics.raw-encoded-json}")
    private String rawEncodedJsonTopic;
}