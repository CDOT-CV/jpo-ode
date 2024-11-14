package us.dot.its.jpo.ode.services.asn1.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.buf.HexUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import us.dot.its.jpo.ode.model.*;
import us.dot.its.jpo.ode.uper.StartFlagNotFoundException;
import us.dot.its.jpo.ode.uper.SupportedMessageType;
import us.dot.its.jpo.ode.uper.UperUtil;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@KafkaListener(id = "MAPJSONListener", topics = "${ode.kafka.topics.raw-encoded-json.map}")
public class Asn1DecodeMAPJSONListener {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ode.kafka.topics.asn1.decoder-input}")
    private String publishTopic;

    private final KafkaTemplate<String, OdeObject> kafkaTemplate;

    public Asn1DecodeMAPJSONListener(KafkaTemplate<String, OdeObject> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaHandler
    public void listen(String consumedData) throws JsonProcessingException, StartFlagNotFoundException {
        log.debug("consumedData: {}", consumedData);
        JSONObject rawMapJsonObject = new JSONObject(consumedData);

        String jsonStringMetadata = rawMapJsonObject.get("metadata").toString();
        OdeMapMetadata metadata = objectMapper.readValue(jsonStringMetadata, OdeMapMetadata.class);

        Asn1Encoding unsecuredDataEncoding = new Asn1Encoding("unsecuredData", "MessageFrame", Asn1Encoding.EncodingRule.UPER);
        metadata.addEncoding(unsecuredDataEncoding);

        String payloadHexString = ((JSONObject) ((JSONObject) rawMapJsonObject.get("payload")).get("data")).getString("bytes");
        payloadHexString = UperUtil.stripDot2Header(payloadHexString, SupportedMessageType.MAP.getStartFlag());

        send(new OdeAsn1Payload(HexUtils.fromHexString(payloadHexString)));
    }

    private void send(OdeObject odeObject) {
        CompletableFuture<SendResult<String, OdeObject>> future = kafkaTemplate.send(publishTopic, odeObject);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error(ex.getMessage(), ex);
            } else {
                log.debug("Successfully sent message to topic {} with offset {} on partition {}",
                        publishTopic, result.getRecordMetadata().offset(), result.getRecordMetadata().partition());
            }
        });
    }
}
