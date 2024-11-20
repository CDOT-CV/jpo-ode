package us.dot.its.jpo.ode.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.state.Stores;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.StreamsBuilderFactoryBeanConfigurer;

import java.util.HashMap;
import java.util.Map;

@EnableKafkaStreams
@Configuration
@Slf4j
public class KafkaStreamConfig {

    private final String timJsonTopic;
    private final OdeKafkaProperties kafkaProperties;

    public KafkaStreamConfig(@Value("${ode.kafka.topics.json.tim}") String timJsonTopic, OdeKafkaProperties odeKafkaProperties) {
        this.timJsonTopic = timJsonTopic;
        this.kafkaProperties = odeKafkaProperties;
    }

    @Bean
    public KafkaStreamsConfiguration defaultKafkaStreamsConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "KeyedOdeTimJson");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, this.kafkaProperties.getBrokers());
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);

        if ("CONFLUENT".equals(kafkaProperties.getKafkaType())) {
            props.put("sasl.jaas.config", this.kafkaProperties.getConfluent().getSaslJaasConfig());
        }
        return new KafkaStreamsConfiguration(props);
    }

    @Bean
    public StreamsBuilderFactoryBeanConfigurer configurer() {
        return fb -> fb.setStateListener(
                (newState, oldState) -> log.debug("State transition from {} to {}", oldState, newState)
        );
    }

    @Bean
    public KStream<String, String> kStream(StreamsBuilder builder) {
        var store = Stores.inMemoryKeyValueStore("timjson-store");
        var storeBuilder = Stores.keyValueStoreBuilder(store, Serdes.String(), Serdes.String());
        builder.addStateStore(storeBuilder);
        return builder.stream(timJsonTopic);
    }
}
