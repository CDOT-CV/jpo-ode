package us.dot.its.jpo.ode.kafka.listeners;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import us.dot.its.jpo.ode.coder.OdeMapDataCreatorHelper;
import us.dot.its.jpo.ode.context.AppContext;
import us.dot.its.jpo.ode.model.OdeAsn1Data;
import us.dot.its.jpo.ode.model.OdeLogMetadata;
import us.dot.its.jpo.ode.util.XmlUtils;

import java.util.Map;

@Slf4j
@Component
@KafkaListener(topics = "${ode.kafka.topics.asn1.decoder-output}", containerFactory = "tempFilteringKafkaListenerContainerFactory")
public class Asn1DecodedDataListener {

    @Value("${ode.kafka.topics.json.map}")
    private String jsonMapTopic;

    @Value("${ode.kafka.topics.pojo.tx-map}")
    private String pojoTxMapTopic;

    KafkaTemplate<String, String> kafkaTemplate;

    public Asn1DecodedDataListener(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaHandler
    public void listenToMAPs(@Headers Map<String, Object> keys, @Payload String payload) {
        log.debug("Key: {} payload: {}", keys, payload);
        try {
            String odeMapData = OdeMapDataCreatorHelper.createOdeMapData(payload).toString();

            OdeLogMetadata.RecordType recordType = OdeLogMetadata.RecordType
                    .valueOf(XmlUtils.toJSONObject(payload)
                            .getJSONObject(OdeAsn1Data.class.getSimpleName())
                            .getJSONObject(AppContext.METADATA_STRING)
                            .getString("recordType")
                    );
            if (recordType == OdeLogMetadata.RecordType.mapTx) {
                log.debug("Publishing message with recordType: {} to {} ", recordType, pojoTxMapTopic);
                kafkaTemplate.send(pojoTxMapTopic, keys.get(KafkaHeaders.RECEIVED_KEY).toString(), odeMapData);
            }

            // Send all MAP data to OdeMapJson despite the record type
            kafkaTemplate.send(jsonMapTopic, keys.get(KafkaHeaders.RECEIVED_KEY).toString(), odeMapData);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
