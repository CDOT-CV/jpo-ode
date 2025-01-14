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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import us.dot.its.jpo.ode.model.OdeBsmData;


@Component
@Slf4j
public class BSMPojoToJSONListener {

  private final String produceTopic;
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper mapper;

  @Autowired
  public BSMPojoToJSONListener(KafkaTemplate<String, String> kafkaTemplate,
                               @Value("${ode.kafka.topics.json.bsm}") String produceTopic,
                               ObjectMapper mapper) {
    super();
    this.kafkaTemplate = kafkaTemplate;
    this.produceTopic = produceTopic;
    this.mapper = mapper;
  }

  @KafkaListener(id = "BSMPojoToJSONListener", topics = "${ode.kafka.topics.pojo.bsm}", containerFactory = "odeBsmDataConsumerListenerContainerFactory")
  public void listen(ConsumerRecord<String, OdeBsmData> consumerRecord) throws JsonProcessingException {
    log.debug("Received record on topic: {} with key: {}", consumerRecord.topic(), consumerRecord.key());
    kafkaTemplate.send(produceTopic, consumerRecord.key(), mapper.writeValueAsString(consumerRecord.value()));
  }
}
