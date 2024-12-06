package us.dot.its.jpo.ode.services.asn1.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.buf.HexUtils;
import org.json.JSONObject;
import us.dot.its.jpo.ode.coder.StringPublisher;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.model.Asn1Encoding;
import us.dot.its.jpo.ode.model.Asn1Encoding.EncodingRule;
import us.dot.its.jpo.ode.model.OdeAsn1Data;
import us.dot.its.jpo.ode.model.OdeAsn1Payload;
import us.dot.its.jpo.ode.model.OdeSsmMetadata;
import us.dot.its.jpo.ode.uper.StartFlagNotFoundException;
import us.dot.its.jpo.ode.uper.SupportedMessageType;
import us.dot.its.jpo.ode.uper.UperUtil;

@Slf4j
public class Asn1DecodeSSMJSON extends AbstractAsn1DecodeMessageJSON {
	private final ObjectMapper objectMapper = new ObjectMapper();

	public Asn1DecodeSSMJSON(OdeKafkaProperties odeKafkaProperties, String publishTopic) {
		super(
				publishTopic,
				new StringPublisher(odeKafkaProperties.getBrokers(), odeKafkaProperties.getKafkaType(), odeKafkaProperties.getDisabledTopics()),
				SupportedMessageType.SSM.getStartFlag()
		);}

	@Override
	protected OdeAsn1Data process(String consumedData) {
		OdeAsn1Data messageToPublish = null;
		try {
			JSONObject rawSsmJsonObject = new JSONObject(consumedData);

			String jsonStringMetadata = rawSsmJsonObject.get("metadata").toString();
			OdeSsmMetadata metadata = objectMapper.readValue(jsonStringMetadata, OdeSsmMetadata.class);

			Asn1Encoding unsecuredDataEncoding = new Asn1Encoding("unsecuredData", "MessageFrame", EncodingRule.UPER);
			metadata.addEncoding(unsecuredDataEncoding);

			String payloadHexString = ((JSONObject) ((JSONObject) rawSsmJsonObject.get("payload")).get("data"))
					.getString("bytes");
			payloadHexString = UperUtil.stripDot2Header(payloadHexString, super.payloadStartFlag);

			OdeAsn1Payload payload = new OdeAsn1Payload(HexUtils.fromHexString(payloadHexString));

			messageToPublish = new OdeAsn1Data(metadata, payload);
			publishEncodedMessageToAsn1Decoder(messageToPublish);
		} catch (StartFlagNotFoundException e) {
			log.error("Unexpected data type encountered.", e);
		} catch (Exception e) {
			log.error("Error publishing to Asn1DecoderInput: {}", e.getMessage());
		}
		return messageToPublish;
	}
}
