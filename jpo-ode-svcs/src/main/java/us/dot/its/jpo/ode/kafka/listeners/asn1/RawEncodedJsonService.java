package us.dot.its.jpo.ode.kafka.listeners.asn1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tomcat.util.buf.HexUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import us.dot.its.jpo.ode.model.Asn1Encoding;
import us.dot.its.jpo.ode.model.Asn1Encoding.EncodingRule;
import us.dot.its.jpo.ode.model.OdeAsn1Data;
import us.dot.its.jpo.ode.model.OdeAsn1Payload;
import us.dot.its.jpo.ode.model.OdeBsmMetadata;
import us.dot.its.jpo.ode.uper.StartFlagNotFoundException;
import us.dot.its.jpo.ode.uper.SupportedMessageType;
import us.dot.its.jpo.ode.uper.UperUtil;

@Service
public class RawEncodedJsonService {

  private final ObjectMapper mapper;

  public RawEncodedJsonService(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public OdeAsn1Data addEncodingAndMutateBytes(String json, SupportedMessageType messageType,
      Class<? extends OdeBsmMetadata> metadataClass)
      throws JsonProcessingException, StartFlagNotFoundException {
    JSONObject rawBsmJsonObject = new JSONObject(json);

    String jsonStringMetadata = rawBsmJsonObject.get("metadata").toString();
    OdeBsmMetadata metadata = mapper.readValue(jsonStringMetadata, metadataClass);

    Asn1Encoding
        unsecuredDataEncoding =
        new Asn1Encoding("unsecuredData", "MessageFrame", EncodingRule.UPER);
    metadata.addEncoding(unsecuredDataEncoding);

    String payloadHexString =
        ((JSONObject) ((JSONObject) rawBsmJsonObject.get("payload")).get("data")).getString(
            "bytes");
    payloadHexString = UperUtil.stripDot2Header(payloadHexString, messageType.getStartFlag());

    OdeAsn1Payload payload = new OdeAsn1Payload(HexUtils.fromHexString(payloadHexString));
    return new OdeAsn1Data(metadata, payload);
  }

}
