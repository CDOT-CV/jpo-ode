package us.dot.its.jpo.ode.kafka.listeners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.buf.HexUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
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

@Slf4j
@Component
@KafkaListener(id = "Asn1DecodeMAPJSONListener", topics = "${ode.kafka.topics.raw-encoded-json.map}")
public class Asn1DecodeMAPJSONListener {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private final String publishTopic;
  private final KafkaTemplate<String, OdeObject> kafkaTemplate;

  public Asn1DecodeMAPJSONListener(KafkaTemplate<String, OdeObject> kafkaTemplate,
      @Value("${ode.kafka.topics.asn1.decoder-input}") String publishTopic) {
    this.kafkaTemplate = kafkaTemplate;
    this.publishTopic = publishTopic;
  }

  @KafkaHandler
  public void listen(String consumedData)
      throws JsonProcessingException, StartFlagNotFoundException {
    log.debug("consumedData: {}", consumedData);
    JSONObject rawMapJsonObject = new JSONObject(consumedData);

    String jsonStringMetadata = rawMapJsonObject.get("metadata").toString();
    OdeMapMetadata metadata = objectMapper.readValue(jsonStringMetadata, OdeMapMetadata.class);

    Asn1Encoding unsecuredDataEncoding =
        new Asn1Encoding("unsecuredData", "MessageFrame", Asn1Encoding.EncodingRule.UPER);
    metadata.addEncoding(unsecuredDataEncoding);

    String payloadHexString =
        ((JSONObject) ((JSONObject) rawMapJsonObject.get("payload")).get("data")).getString(
            "bytes");
    payloadHexString =
        UperUtil.stripDot2Header(payloadHexString, SupportedMessageType.MAP.getStartFlag());

    OdeAsn1Payload payload = new OdeAsn1Payload(HexUtils.fromHexString(payloadHexString));

    OdeAsn1Data data = new OdeAsn1Data(metadata, payload);
    kafkaTemplate.send(publishTopic, data);
  }
}
