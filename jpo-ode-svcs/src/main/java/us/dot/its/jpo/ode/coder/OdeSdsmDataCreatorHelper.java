package us.dot.its.jpo.ode.coder;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import us.dot.its.jpo.asn.j2735.r2024.SensorDataSharingMessage.SensorDataSharingMessage;
import us.dot.its.jpo.ode.context.AppContext;
import us.dot.its.jpo.ode.model.OdeSdsmData;
import us.dot.its.jpo.ode.model.OdeSdsmMetadata;
import us.dot.its.jpo.ode.model.OdeSdsmPayload;
import us.dot.its.jpo.ode.model.ReceivedMessageDetails;
import us.dot.its.jpo.ode.model.RxSource;
import us.dot.its.jpo.ode.util.JsonUtils;
import us.dot.its.jpo.ode.util.XmlUtils;
import us.dot.its.jpo.ode.util.XmlUtils.XmlUtilsException;

public class OdeSdsmDataCreatorHelper {

	public OdeSdsmDataCreatorHelper() {
	}

	public static OdeSdsmData createOdeSdsmData(String consumedData) throws XmlUtilsException, JsonProcessingException, IOException {
		ObjectNode consumed = XmlUtils.toObjectNode(consumedData);

		JsonNode metadataNode = consumed.findValue(AppContext.METADATA_STRING);
		if (metadataNode instanceof ObjectNode) {
			ObjectNode object = (ObjectNode) metadataNode;
			object.remove(AppContext.ENCODINGS_STRING);

			// Sdsm header file does not have a location and use predefined set required
			// RxSource
			ReceivedMessageDetails receivedMessageDetails = new ReceivedMessageDetails();
			receivedMessageDetails.setRxSource(RxSource.NA);
			ObjectMapper objectSdsmper = new ObjectMapper();
			JsonNode jsonNode;
			try {
				jsonNode = objectSdsmper.readTree(receivedMessageDetails.toJson());
				object.set(AppContext.RECEIVEDMSGDETAILS_STRING, jsonNode);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		OdeSdsmMetadata metadata = (OdeSdsmMetadata) JsonUtils.fromJson(metadataNode.toString(), OdeSdsmMetadata.class);

		if (metadata.getSchemaVersion() <= 4) {
			metadata.setReceivedMessageDetails(null);
		}
		
		XmlMapper xmlMapper = new XmlMapper();
		String sdsmString = XmlUtils.findXmlContentString(consumedData, "SensorDataSharingMessage");
		SensorDataSharingMessage sdsmObject = xmlMapper.readValue(sdsmString, SensorDataSharingMessage.class);

		OdeSdsmPayload payload = new OdeSdsmPayload(sdsmObject);
		return new OdeSdsmData(metadata, payload);
	}
}
