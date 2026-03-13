package us.dot.its.jpo.ode.traveler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(value = TimIngestTrackerProperties.class)
class TimIngestTrackerPropertiesTest {

    @Autowired
    TimIngestTrackerProperties timIngestTrackerProperties;

    @Test
    void isTrackingEnabled() {
        assertTrue(timIngestTrackerProperties.isTrackingEnabled());
    }

    @Test
    void getMonitoringInterval() {
        assertEquals(10, timIngestTrackerProperties.getInterval());
    }
}