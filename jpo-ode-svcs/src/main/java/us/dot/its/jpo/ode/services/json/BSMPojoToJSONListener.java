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
package us.dot.its.jpo.ode.services.json;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.kafka.topics.JsonTopics;
import us.dot.its.jpo.ode.kafka.topics.PojoTopics;
import us.dot.its.jpo.ode.model.OdeBsmData;

/**
 * Launches ToJsonConverter service
 */
@Component
@Slf4j
public class BSMPojoToJSONListener {

   @Autowired
   public BSMPojoToJSONListener(OdeKafkaProperties odeKafkaProperties, JsonTopics jsonTopics, PojoTopics pojoTopics) {
      super();

   }

   @KafkaListener(id = "BSMPojoToJSONListener", topics = "${ode.kafka.topics.pojo.bsm}")
   public void listen(ConsumerRecord<String, OdeBsmData> consumerRecord) {
      log.debug("Received record on topic: {} with key: {} and value: {}", consumerRecord.topic(), consumerRecord.key(), consumerRecord.value());
   }
}
