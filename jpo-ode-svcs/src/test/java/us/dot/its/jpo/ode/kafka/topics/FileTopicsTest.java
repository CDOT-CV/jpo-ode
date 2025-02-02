package us.dot.its.jpo.ode.kafka.topics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@EnableConfigurationProperties(value = FileTopics.class)
class FileTopicsTest {

    @Autowired
    FileTopics fileTopics;

    @Test
    void getFilteredOutput() {
        assertEquals("/topic/filtered_messages", fileTopics.getFilteredOutput());
    }

    @Test
    void getUnfilteredOutput() {
        assertEquals("/topic/unfiltered_messages", fileTopics.getUnfilteredOutput());
    }
}