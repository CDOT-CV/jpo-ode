package us.dot.its.jpo.ode.kafka.producer;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.kafka.mock.MockProducerFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tests for the per-RSU message counter in {@link InterceptingKafkaTemplate}, which is tagged with
 * the {@code metadata.originIp} of each produced JSON message.
 */
class InterceptingKafkaTemplateTest {

  private static final String TOPIC = "topic.Test";
  private static final String RSU_COUNTER = "kafka.produced.rsu.messages";

  private SimpleMeterRegistry meterRegistry;
  private MockProducer<String, String> producer;
  private InterceptingKafkaTemplate<String, String> template;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    producer = new MockProducer<>(true, null, new StringSerializer(), new StringSerializer());
    template = new InterceptingKafkaTemplate<>(
        new MockProducerFactory<>(() -> producer),
        Set.of(),
        meterRegistry,
        JsonMapper.builderWithJackson2Defaults().build());
  }

  private void send(String value) {
    // Explicit partition, so the MockProducer doesn't need a partitioner or cluster metadata
    template.send(TOPIC, 0, null, value).join();
  }

  @Test
  void rsuCounterIsTaggedWithOriginIp() {
    send("""
        {
          "metadata": {
            "originIp": "10.0.0.1"
          },
          "payload": {}
        }
        """);

    Counter counter = meterRegistry.find(RSU_COUNTER)
        .tags("topic", TOPIC, "rsu_ip", "10.0.0.1")
        .counter();
    assertThat(counter).isNotNull();
    assertThat(counter.count()).isEqualTo(1.0);
    assertThat(producer.history()).hasSize(1);
  }

  static Stream<Arguments> messagesWithoutAStringOriginIp() {
    return Stream.of(
        Arguments.of(Named.of("null originIp", """
            {
              "metadata": {
                "originIp": null
              }
            }
            """)),
        Arguments.of(Named.of("empty originIp", """
            {
              "metadata": {
                "originIp": ""
              }
            }
            """)),
        Arguments.of(Named.of("blank originIp", """
            {
              "metadata": {
                "originIp": "  "
              }
            }
            """)),
        Arguments.of(Named.of("object originIp", """
            {
              "metadata": {
                "originIp": {
                  "ip": "10.0.0.1"
                }
              }
            }
            """)),
        Arguments.of(Named.of("array originIp", """
            {
              "metadata": {
                "originIp": ["10.0.0.1"]
              }
            }
            """)),
        Arguments.of(Named.of("number originIp", """
            {
              "metadata": {
                "originIp": 5
              }
            }
            """)),
        Arguments.of(Named.of("no originIp", """
            {
              "metadata": {}
            }
            """)),
        Arguments.of(Named.of("no metadata", """
            {
              "payload": {}
            }
            """)),
        Arguments.of(Named.of("not JSON", "not json")));
  }

  @ParameterizedTest
  @MethodSource("messagesWithoutAStringOriginIp")
  void rsuCounterIsNotRegisteredWithoutAStringOriginIp(String value) {
    send(value);

    assertThat(meterRegistry.find(RSU_COUNTER).counters()).isEmpty();
    // The message itself is still produced
    assertThat(producer.history()).hasSize(1);
  }
}
