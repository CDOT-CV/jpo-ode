package us.dot.its.jpo.ode.kafka.listeners.asn1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.tomcat.util.buf.HexUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import us.dot.its.jpo.ode.model.Asn1Encoding;
import us.dot.its.jpo.ode.model.OdeAsn1Data;
import us.dot.its.jpo.ode.model.OdeAsn1Payload;
import us.dot.its.jpo.ode.model.OdeMapMetadata;
import us.dot.its.jpo.ode.model.OdeObject;
import us.dot.its.jpo.ode.uper.StartFlagNotFoundException;
import us.dot.its.jpo.ode.uper.SupportedMessageType;
import us.dot.its.jpo.ode.uper.UperUtil;

/**
 * A Kafka listener component that processes ASN.1 encoded MAP JSON messages from a specified Kafka
 * topic. It processes the raw encoded JSON messages and publishes them to be decoded by the ASN.1
 * codec
 */
@Slf4j
@Component
public class RawEncodedMAPJsonRouter {

  private final ObjectMapper mapper;
  private final KafkaTemplate<String, OdeObject> kafkaTemplate;
  private final String publishTopic;
  private static final SupportedMessageType MESSAGE_TYPE = SupportedMessageType.MAP;

  /**
   * Constructor for the RawEncodedMAPJsonRouter class.
   *
   * @param kafkaTemplate The KafkaTemplate instance used to publish decoded data to the specified
   *                      Kafka topic.
   * @param publishTopic  The Kafka topic to which the decoded and processed data is published.
   * @param mapper        The ObjectMapper instance used for JSON serialization and
   *                      deserialization.
   */
  public RawEncodedMAPJsonRouter(KafkaTemplate<String, OdeObject> kafkaTemplate,
      @Value("${ode.kafka.topics.asn1.decoder-input}") String publishTopic,
      ObjectMapper mapper) {
    this.kafkaTemplate = kafkaTemplate;
    this.publishTopic = publishTopic;
    this.mapper = mapper;
  }

  /**
   * Consumes and processes Kafka messages containing ASN.1 encoded MAP JSON data. This method
   * extracts metadata and payload from the JSON message sends it for decoding.
   *
   * @param consumerRecord The Kafka consumer record containing the message key and value. The value
   *                       includes the raw ASN.1 encoded JSON MAP data to be processed.
   * @throws StartFlagNotFoundException If the start flag for the MAP message type is not found
   *                                    during payload processing.
   * @throws JsonProcessingException    If there's an error while processing or deserializing JSON
   *                                    data.
   */
  @KafkaListener(id = "RawEncodedMAPJsonRouter", topics = "${ode.kafka.topics.raw-encoded-json.map}")
  public void listen(ConsumerRecord<String, String> consumerRecord)
      throws JsonProcessingException, StartFlagNotFoundException {
    JSONObject rawMapJsonObject = new JSONObject(consumerRecord.value());

    String jsonStringMetadata = rawMapJsonObject.get("metadata").toString();
    OdeMapMetadata metadata = mapper.readValue(jsonStringMetadata, OdeMapMetadata.class);

    Asn1Encoding unsecuredDataEncoding =
        new Asn1Encoding("unsecuredData", "MessageFrame", Asn1Encoding.EncodingRule.UPER);
    metadata.addEncoding(unsecuredDataEncoding);

    String payloadHexString =
        ((JSONObject) ((JSONObject) rawMapJsonObject.get("payload")).get("data")).getString(
            "bytes");
    payloadHexString =
        UperUtil.stripDot2Header(payloadHexString, MESSAGE_TYPE.getStartFlag());

    OdeAsn1Payload payload = new OdeAsn1Payload(HexUtils.fromHexString(payloadHexString));

    OdeAsn1Data data = new OdeAsn1Data(metadata, payload);
    kafkaTemplate.send(publishTopic, consumerRecord.key(), data);
  }
}
