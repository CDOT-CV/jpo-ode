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

import lombok.Getter;
import lombok.Setter;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.util.SerializableObjectPool;
import us.dot.its.jpo.ode.wrapper.MessageProducer;

import java.util.Properties;

@Getter
@Setter
public class SerializableMessageProducerPool<K, V> extends SerializableObjectPool<MessageProducer<K, V>> {

    private static final long serialVersionUID = -2293786403623236678L;

    transient OdeKafkaProperties odeKafkaProperties;

    private String brokers;
    private String type;
    private String partitionerClass;

    private Properties props;

    public SerializableMessageProducerPool(OdeKafkaProperties odeKafkaProperties) {
        super();
        this.odeKafkaProperties = odeKafkaProperties;
        this.brokers = odeKafkaProperties.getBrokers();
        this.type = odeKafkaProperties.getProducer().getType();
        this.partitionerClass = odeKafkaProperties.getProducer().getPartitionerClass();
        init();
    }

    protected SerializableMessageProducerPool<K, V> init() {
        props = new Properties();
        // Set acknowledgments for producer requests.
        props.put("acks", odeKafkaProperties.getProducer().getAcks());
        // If the request fails, the producer can automatically retry
        props.put("retries", odeKafkaProperties.getProducer().getRetries());
        props.put("batch.size", odeKafkaProperties.getProducer().getBatchSize());
        props.put("linger.ms", odeKafkaProperties.getProducer().getLingerMs());
        props.put("buffer.memory", odeKafkaProperties.getProducer().getBufferMemory());
        props.put("key.serializer", odeKafkaProperties.getProducer().getKeySerializer());
        props.put("value.serializer", odeKafkaProperties.getProducer().getValueSerializer());

        return this;
    }

    @Override
    protected MessageProducer<K, V> create() {
        return new MessageProducer<>(brokers, type, partitionerClass, props,
                odeKafkaProperties.getDisabledTopics());
    }

    @Override
    public boolean validate(MessageProducer<K, V> o) {
        return o.getProducer() != null;
    }

    @Override
    public void expire(MessageProducer<K, V> o) {
        o.close();
    }
}
