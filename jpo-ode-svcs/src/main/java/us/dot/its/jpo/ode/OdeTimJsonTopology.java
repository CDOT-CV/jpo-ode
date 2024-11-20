package us.dot.its.jpo.ode;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.Stores;
import org.springframework.stereotype.Component;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;

import java.util.Properties;


/**
 * The OdeTimJsonTopology class sets up and manages a Kafka Streams topology
 * for processing TIM (Traveler Information Message) JSON data from the OdeTimJson Kafka topic.
 * This class creates a K-Table that houses TMC-generated TIMs which can be queried by UUID.
 **/
@Slf4j
@Component
public class OdeTimJsonTopology {

    private final KafkaStreams streams;

    public OdeTimJsonTopology(OdeKafkaProperties odeKafkaProps) {
        Properties streamsProperties = new Properties();
        if (odeKafkaProps.getBrokers() != null) {
            streamsProperties.put(StreamsConfig.APPLICATION_ID_CONFIG, "KeyedOdeTimJson");
            streamsProperties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, odeKafkaProps.getBrokers());
            streamsProperties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
            streamsProperties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class);
    
            String kafkaType = System.getenv("KAFKA_TYPE");
            if (kafkaType != null && kafkaType.equals("CONFLUENT")) {
                addConfluentProperties(streamsProperties);
            }  
        }  else {
            log.error("Kafka Brokers not set in OdeProperties");
        }
        streams = new KafkaStreams(buildTopology(), streamsProperties);
        streams.start();
    }

    public Topology buildTopology() {
        StreamsBuilder builder = new StreamsBuilder();
        builder.table("topic.OdeTimJson", Materialized.<String, String>as(Stores.inMemoryKeyValueStore("timjson-store")));
        return builder.build();
    }

    public String query(String uuid) {
        return (String) streams.store(StoreQueryParameters.fromNameAndType("timjson-store", QueryableStoreTypes.keyValueStore())).get(uuid);
    }

    private void addConfluentProperties(Properties properties) {
        String username = System.getenv("CONFLUENT_KEY");
        String password = System.getenv("CONFLUENT_SECRET");

        if (username != null && password != null) {
            String auth = "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                "username=\"" + username + "\" " +
                "password=\"" + password + "\";";
          properties.put("sasl.jaas.config", auth);
        }
        else {
            log.error("Environment variables CONFLUENT_KEY and CONFLUENT_SECRET are not set. Set these in the .env file to use Confluent Cloud");
        }
    }
}
