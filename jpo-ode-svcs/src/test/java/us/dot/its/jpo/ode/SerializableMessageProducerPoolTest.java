/*******************************************************************************
 * Copyright 2018 572682
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package us.dot.its.jpo.ode;

import org.apache.kafka.clients.producer.Producer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.wrapper.MessageProducer;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@Import({BuildProperties.class})
@EnableConfigurationProperties(value = {OdeProperties.class, OdeKafkaProperties.class})
class SerializableMessageProducerPoolTest {

    @Autowired
    OdeProperties testOdeProperties;
    @Autowired
    OdeKafkaProperties testOdeKafkaProperties;

    @Test
    void testSerializableMessageProducerPool() {
        SerializableMessageProducerPool<String, String> testSerializableMessageProducerPool = new SerializableMessageProducerPool<>(testOdeProperties, testOdeKafkaProperties);

        assertEquals(testOdeKafkaProperties.getBrokers(), testSerializableMessageProducerPool.getBrokers());
        assertEquals(testOdeKafkaProperties.getProducer().getType(), testSerializableMessageProducerPool.getType());
        assertEquals(testOdeKafkaProperties.getProducer().getPartitionerClass(), testSerializableMessageProducerPool.getPartitionerClass());
        assertEquals(testOdeKafkaProperties.getProducer().getAcks(), testSerializableMessageProducerPool.getProps().get("acks"));
        assertEquals(testOdeKafkaProperties.getProducer().getRetries(), testSerializableMessageProducerPool.getProps().get("retries"));
        assertEquals(testOdeKafkaProperties.getProducer().getBatchSize(), testSerializableMessageProducerPool.getProps().get("batch.size"));
        assertEquals(testOdeKafkaProperties.getProducer().getLingerMs(), testSerializableMessageProducerPool.getProps().get("linger.ms"));
        assertEquals(testOdeKafkaProperties.getProducer().getBufferMemory(), testSerializableMessageProducerPool.getProps().get("buffer.memory"));
        assertEquals(testOdeKafkaProperties.getProducer().getKeySerializer(), testSerializableMessageProducerPool.getProps().get("key.serializer"));
        assertEquals(testOdeKafkaProperties.getProducer().getValueSerializer(), testSerializableMessageProducerPool.getProps().get("value.serializer"));
    }

    @Test
    void testCreate() {
        SerializableMessageProducerPool<String, String> testSerializableMessageProducerPool = new SerializableMessageProducerPool<>(testOdeProperties, testOdeKafkaProperties);
        assertEquals(MessageProducer.class, testSerializableMessageProducerPool.create().getClass());
    }

    @Test
    void testValidate() {
        SerializableMessageProducerPool<String, String> testSerializableMessageProducerPool = new SerializableMessageProducerPool<>(testOdeProperties, testOdeKafkaProperties);
        MessageProducer<String, String> producer = testSerializableMessageProducerPool.create();
        assertTrue(testSerializableMessageProducerPool.validate(producer));
    }

    @Test
    void testExpire() {
        SerializableMessageProducerPool<String, String> testSerializableMessageProducerPool = new SerializableMessageProducerPool<>(testOdeProperties, testOdeKafkaProperties);
        MessageProducer<String, String> producer = testSerializableMessageProducerPool.create();
        testSerializableMessageProducerPool.expire(producer);

        // To confirm that the producer has been expired, we will try to send a message.
        // If the producer has been expired, it will throw an IllegalStateException.
        Producer<String, String> internalProducer = producer.getProducer();
        assertThrows(IllegalStateException.class, () -> internalProducer.send(null));
    }
}
