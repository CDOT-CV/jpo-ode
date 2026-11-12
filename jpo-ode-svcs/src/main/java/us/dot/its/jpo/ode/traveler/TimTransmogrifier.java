package us.dot.its.jpo.ode.traveler;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerInformation;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerInformationMessageFrame;
import us.dot.its.jpo.ode.model.Asn1Encoding;
import us.dot.its.jpo.ode.model.Asn1Encoding.EncodingRule;
import us.dot.its.jpo.ode.model.OdeAsn1Data;
import us.dot.its.jpo.ode.model.OdeMsgMetadata;
import us.dot.its.jpo.ode.model.OdeMsgPayload;
import us.dot.its.jpo.ode.model.OdeTimPayload;
import us.dot.its.jpo.ode.model.SerialId;
import us.dot.its.jpo.ode.plugin.RoadSideUnit.RSU;
import us.dot.its.jpo.ode.plugin.ServiceRequest;
import us.dot.its.jpo.ode.rsu.RsuProperties;
import us.dot.its.jpo.ode.util.JsonUtils;
import us.dot.its.jpo.ode.util.JsonUtils.JsonUtilsException;
import us.dot.its.jpo.ode.util.XmlUtils;
import us.dot.its.jpo.ode.util.XmlUtils.XmlUtilsException;

public class TimTransmogrifier {

   private static final String MESSAGE_FRAME = "MessageFrame";

   public static final String RSUS_STRING = "rsus";
   public static final String REQUEST_STRING = "request";

   private TimTransmogrifier() {
      throw new UnsupportedOperationException();
   }

   public static String obfuscateRsuPassword(String message) {
      return message.replaceAll("\"rsuPassword\": *\".*?\"", "\"rsuPassword\":\"*\"");
   }

   public static String convertToXml(TravelerInformation tim, ServiceRequest serviceRequest,
         OdeMsgMetadata timMetadata, SerialId serialIdJ2735) throws JsonUtilsException, XmlUtilsException {

      TravelerInformationMessageFrame messageFrame = new TravelerInformationMessageFrame();
      messageFrame.setValue(tim);
      ObjectNode dataBodyObj = JsonUtils.newNode();
      dataBodyObj.putPOJO(MESSAGE_FRAME, messageFrame);

      OdeMsgPayload payload = new OdeTimPayload();
      payload.setDataType(MESSAGE_FRAME);

      ObjectNode payloadObj = JsonUtils.toObjectNode(payload.toJson());
      payloadObj.set(OdeMsgPayload.DATA_STRING, dataBodyObj);

      // Create a valid metadata from scratch
      OdeMsgMetadata metadata = new OdeMsgMetadata(payload);
      metadata.setSerialId(serialIdJ2735);
      metadata.setRecordGeneratedBy(timMetadata.getRecordGeneratedBy());
      metadata.setRecordGeneratedAt(timMetadata.getRecordGeneratedAt());
      metadata.setSchemaVersion(timMetadata.getSchemaVersion());
      metadata.setMaxDurationTime(timMetadata.getMaxDurationTime());
      metadata.setOdePacketID(timMetadata.getOdePacketID());
      metadata.setOdeTimStartDateTime(timMetadata.getOdeTimStartDateTime());
      ObjectNode metaObject = JsonUtils.toObjectNode(metadata.toJson());

      ObjectNode request = JsonUtils.toObjectNode(serviceRequest.toJson());
      metaObject.set(REQUEST_STRING, request);

      if (request.has(RSUS_STRING)) {
         convertRsusArray(request);
      }

      // Add 'encodings' array to metadata
      convertEncodingsArray(metaObject);

      ObjectNode message = JsonUtils.newNode();
      message.set(OdeMsgMetadata.METADATA_STRING, metaObject);
      message.set(OdeMsgPayload.PAYLOAD_STRING, payloadObj);

      ObjectNode root = JsonUtils.newNode();
      root.set(OdeAsn1Data.ODE_ASN1_DATA, message);

      // Convert to XML
      String outputXml = XmlUtils.toXmlStatic(root);

      // remove the surrounding <ObjectNode></ObjectNode>
      String fixedXml = outputXml.replace("<ObjectNode>", "");
      fixedXml = fixedXml.replace("</ObjectNode>", "");

      return fixedXml;
   }

   private static void convertEncodingsArray(ObjectNode metaObject)
         throws JsonUtilsException {
      ArrayNode encodings = buildEncodings();
      ObjectNode enc = XmlUtils.createEmbeddedJsonArrayForXmlConversion(OdeMsgMetadata.ENCODINGS_STRING, encodings);
      metaObject.set(OdeMsgMetadata.ENCODINGS_STRING, enc);
   }

   private static void convertRsusArray(ObjectNode request) {
      // Convert 'rsus' JSON array to XML array
      ObjectNode rsus = XmlUtils.createEmbeddedJsonArrayForXmlConversion(RSUS_STRING,
            (ArrayNode) request.get(RSUS_STRING));
      request.set(RSUS_STRING, rsus);
   }

   private static ArrayNode buildEncodings() throws JsonUtilsException {
      ArrayNode encodings = JsonUtils.newArrayNode();
      encodings.add(buildEncodingNode(MESSAGE_FRAME, MESSAGE_FRAME, EncodingRule.UPER));
      return encodings;
   }

   public static void updateRsuCreds(RSU rsu, RsuProperties rsuProperties) {

      if (rsu.getRsuUsername() == null || rsu.getRsuUsername().isEmpty()) {
         rsu.setRsuUsername(rsuProperties.getUsername());
      }

      if (rsu.getRsuPassword() == null || rsu.getRsuPassword().isEmpty()) {
         rsu.setRsuPassword(rsuProperties.getPassword());
      }
   }

   public static JsonNode buildEncodingNode(String name, String type, EncodingRule rule) throws JsonUtilsException {
      Asn1Encoding mfEnc = new Asn1Encoding(name, type, rule);
      return JsonUtils.toObjectNode(mfEnc.toJson());
   }
}
