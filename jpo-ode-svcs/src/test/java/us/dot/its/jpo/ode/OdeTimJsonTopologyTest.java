package us.dot.its.jpo.ode;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(value = OdeKafkaProperties.class)
class OdeTimJsonTopologyTest {

    private OdeTimJsonTopology odeTimJsonTopology;
    @Autowired
    private OdeKafkaProperties odeKafkaProperties;
    @Value("${ode.kafka.topics.json.tim}")
    private String timTopic;

    @BeforeEach
    void setUp() throws SecurityException, IllegalArgumentException {
        odeTimJsonTopology = new OdeTimJsonTopology(odeKafkaProperties, timTopic);
    }

    @Test
    void testStop() {
        odeTimJsonTopology.stop();
        assertFalse(odeTimJsonTopology.isRunning());
    }

    @Test
    void testIsRunning() {
        assertTrue(odeTimJsonTopology.isRunning());
    }

    @Test
    void testIsNotRunning() {
        odeTimJsonTopology.stop();
        Awaitility.setDefaultTimeout(15000, java.util.concurrent.TimeUnit.MILLISECONDS);
        Awaitility.await().untilAsserted( () -> assertFalse(odeTimJsonTopology.isRunning()));
    }

//    @Test
//    void testQuery() {
//        String uuid = "test-uuid";
//        String expectedValue = "test-value";
//
//        when(mockStreams.store(any(StoreQueryParameters.class))).thenReturn(mockStore);
//        when(mockStore.get(uuid)).thenReturn(expectedValue);
//
//        String result = odeTimJsonTopology.query(uuid);
//
//        assertEquals(expectedValue, result);
//    }
}