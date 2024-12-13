package us.dot.its.jpo.ode.kafka.listeners.asn1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.tomcat.util.buf.HexUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import us.dot.its.jpo.ode.model.Asn1Encoding;
import us.dot.its.jpo.ode.model.Asn1Encoding.EncodingRule;
import us.dot.its.jpo.ode.model.OdeAsn1Data;
import us.dot.its.jpo.ode.model.OdeAsn1Payload;
import us.dot.its.jpo.ode.model.OdeBsmMetadata;
import us.dot.its.jpo.ode.model.OdeObject;
import us.dot.its.jpo.ode.uper.StartFlagNotFoundException;
import us.dot.its.jpo.ode.uper.SupportedMessageType;
import us.dot.its.jpo.ode.uper.UperUtil;

/**
 * A Kafka listener component that processes ASN.1 encoded BSM JSON messages from a specified Kafka
 * topic. It processes the raw encoded JSON messages and publishes them to be decoded by the ASN.1
 * codec
 */
@Component
public class RawEncodedBSMJsonListener {

  private final ObjectMapper mapper;
  private final KafkaTemplate<String, OdeObject> kafkaTemplate;
  private final String publishTopic;
  private static final SupportedMessageType MESSAGE_TYPE = SupportedMessageType.BSM;

  /**
   * Constructs an instance of the RawEncodedBSMJsonListener.
   *
   * @param mapper        An instance of ObjectMapper used for JSON serialization and
   *                      deserialization.
   * @param kafkaTemplate A KafkaTemplate for publishing messages to a Kafka topic.
   * @param publishTopic  The name of the Kafka topic to publish the processed messages to.
   */
  public RawEncodedBSMJsonListener(ObjectMapper mapper,
      KafkaTemplate<String, OdeObject> kafkaTemplate,
      @Value("${ode.kafka.topics.asn1.decoder-input}") String publishTopic) {
    this.mapper = mapper;
    this.kafkaTemplate = kafkaTemplate;
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
  @KafkaListener(id = "RawEncodedBSMJsonListener", topics = "${ode.kafka.topics.raw-encoded-json.bsm}")
  public void listen(ConsumerRecord<String, String> consumerRecord)
      throws StartFlagNotFoundException, JsonProcessingException {
    JSONObject rawBsmJsonObject = new JSONObject(consumerRecord.value());

    String jsonStringMetadata = rawBsmJsonObject.get("metadata").toString();
    OdeBsmMetadata metadata = mapper.readValue(jsonStringMetadata, OdeBsmMetadata.class);

    Asn1Encoding
        unsecuredDataEncoding =
        new Asn1Encoding("unsecuredData", "MessageFrame", EncodingRule.UPER);
    metadata.addEncoding(unsecuredDataEncoding);

    String payloadHexString =
        ((JSONObject) ((JSONObject) rawBsmJsonObject.get("payload")).get("data")).getString(
            "bytes");
    payloadHexString = UperUtil.stripDot2Header(payloadHexString, MESSAGE_TYPE.getStartFlag());

    OdeAsn1Payload payload = new OdeAsn1Payload(HexUtils.fromHexString(payloadHexString));

    var messageToPublish = new OdeAsn1Data(metadata, payload);
    kafkaTemplate.send(publishTopic, consumerRecord.key(), messageToPublish);
  }
}
