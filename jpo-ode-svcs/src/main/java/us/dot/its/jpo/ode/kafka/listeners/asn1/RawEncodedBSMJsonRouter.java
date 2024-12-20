package us.dot.its.jpo.ode.kafka.listeners.asn1;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import us.dot.its.jpo.ode.model.OdeBsmMetadata;
import us.dot.its.jpo.ode.model.OdeObject;
import us.dot.its.jpo.ode.uper.StartFlagNotFoundException;
import us.dot.its.jpo.ode.uper.SupportedMessageType;

/**
 * A Kafka listener component that processes ASN.1 encoded BSM JSON messages from a specified Kafka
 * topic. It processes the raw encoded JSON messages and publishes them to be decoded by the ASN.1
 * codec
 */
@Component
public class RawEncodedBSMJsonRouter {

  private final KafkaTemplate<String, OdeObject> kafkaTemplate;
  private final String publishTopic;
  private final RawEncodedJsonService rawEncodedJsonService;

  /**
   * Constructs an instance of the RawEncodedBSMJsonRouter.
   *
   * @param kafkaTemplate A KafkaTemplate for publishing messages to a Kafka topic.
   * @param publishTopic  The name of the Kafka topic to publish the processed messages to.
   */
  public RawEncodedBSMJsonRouter(KafkaTemplate<String, OdeObject> kafkaTemplate,
      RawEncodedJsonService rawEncodedJsonService,
      @Value("${ode.kafka.topics.asn1.decoder-input}") String publishTopic) {
    this.kafkaTemplate = kafkaTemplate;
    this.rawEncodedJsonService = rawEncodedJsonService;
    this.publishTopic = publishTopic;
  }

  /**
   * Consumes and processes Kafka messages containing ASN.1 encoded BSM JSON data. This method
   * extracts metadata and payload from the JSON message sends it for decoding.
   *
   * @param consumerRecord The Kafka consumer record containing the message key and value. The value
   *                       includes the raw ASN.1 encoded JSON BSM data to be processed.
   * @throws StartFlagNotFoundException If the start flag for the BSM message type is not found
   *                                    during payload processing.
   * @throws JsonProcessingException    If there's an error while processing or deserializing JSON
   *                                    data.
   */
  @KafkaListener(id = "RawEncodedBSMJsonRouter", topics = "${ode.kafka.topics.raw-encoded-json.bsm}")
  public void listen(ConsumerRecord<String, String> consumerRecord)
      throws StartFlagNotFoundException, JsonProcessingException {
    var messageToPublish =
        rawEncodedJsonService.addEncodingAndMutateBytes(consumerRecord.value(),
            SupportedMessageType.BSM,
            OdeBsmMetadata.class);
    kafkaTemplate.send(publishTopic, consumerRecord.key(), messageToPublish);
  }
}
