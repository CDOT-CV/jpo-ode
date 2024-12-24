/*=============================================================================
 * Copyright 2018 572682
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package us.dot.its.jpo.ode.kafka.listeners.asn1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import us.dot.its.jpo.ode.OdeTimJsonTopology;
import us.dot.its.jpo.ode.context.AppContext;
import us.dot.its.jpo.ode.kafka.topics.Asn1CoderTopics;
import us.dot.its.jpo.ode.kafka.topics.JsonTopics;
import us.dot.its.jpo.ode.model.Asn1Encoding.EncodingRule;
import us.dot.its.jpo.ode.model.OdeAsdPayload;
import us.dot.its.jpo.ode.model.OdeAsn1Data;
import us.dot.its.jpo.ode.model.OdeMsgMetadata;
import us.dot.its.jpo.ode.model.OdeMsgPayload;
import us.dot.its.jpo.ode.plugin.SNMP;
import us.dot.its.jpo.ode.plugin.ServiceRequest;
import us.dot.its.jpo.ode.plugin.SituationDataWarehouse.SDW;
import us.dot.its.jpo.ode.plugin.j2735.DdsAdvisorySituationData;
import us.dot.its.jpo.ode.plugin.j2735.builders.GeoRegionBuilder;
import us.dot.its.jpo.ode.rsu.RsuDepositor;
import us.dot.its.jpo.ode.security.ISecurityServicesClient;
import us.dot.its.jpo.ode.security.SecurityServicesProperties;
import us.dot.its.jpo.ode.traveler.TimTransmogrifier;
import us.dot.its.jpo.ode.uper.SupportedMessageType;
import us.dot.its.jpo.ode.util.CodecUtils;
import us.dot.its.jpo.ode.util.JsonUtils;
import us.dot.its.jpo.ode.util.JsonUtils.JsonUtilsException;
import us.dot.its.jpo.ode.util.XmlUtils;
import us.dot.its.jpo.ode.util.XmlUtils.XmlUtilsException;

/**
 * The Asn1EncodedDataRouter is responsible for routing encoded TIM messages that are consumed from
 * the Kafka topic.Asn1EncoderOutput topic and decide whether to route to the SDX or an RSU.
 **/
@Component
@Slf4j
public class Asn1EncodedDataRouter {

  private static final String BYTES = "bytes";
  private static final String MESSAGE_FRAME = "MessageFrame";
  private static final String ERROR_ON_SDX_DEPOSIT = "Error on SDX deposit.";
  private static final String ADVISORY_SITUATION_DATA_STRING = "AdvisorySituationData";
  private final KafkaTemplate<String, String> kafkaTemplate;

  /**
   * Exception for Asn1EncodedDataRouter specific failures.
   */
  public static class Asn1EncodedDataRouterException extends Exception {
    public Asn1EncodedDataRouterException(String string) {
      super(string);
    }
  }

  private final Asn1CoderTopics asn1CoderTopics;
  private final JsonTopics jsonTopics;
  private final String sdxDepositTopic;
  private final ISecurityServicesClient securityServicesClient;
  private final ObjectMapper mapper;

  private final OdeTimJsonTopology odeTimJsonTopology;
  private final RsuDepositor rsuDepositor;
  private final boolean dataSigningEnabledSDW;
  private final boolean dataSigningEnabledRSU;

  /**
   * Instantiates the Asn1EncodedDataRouter to actively consume from Kafka and route the encoded TIM
   * messages to the SDX and RSUs.
   *
   * @param asn1CoderTopics            The specified ASN1 Coder topics
   * @param jsonTopics                 The specified JSON topics to write to
   * @param securityServicesProperties The security services properties to use
   * @param mapper                     The ObjectMapper used for serialization/deserialization
   **/
  public Asn1EncodedDataRouter(Asn1CoderTopics asn1CoderTopics,
                               JsonTopics jsonTopics,
                               SecurityServicesProperties securityServicesProperties,
                               OdeTimJsonTopology odeTimJsonTopology,
                               RsuDepositor rsuDepositor,
                               ISecurityServicesClient securityServicesClient,
                               KafkaTemplate<String, String> kafkaTemplate,
                               @Value("${ode.kafka.topics.sdx-depositor.input}") String sdxDepositTopic,
                               ObjectMapper mapper) {
    super();

    this.asn1CoderTopics = asn1CoderTopics;
    this.jsonTopics = jsonTopics;
    this.sdxDepositTopic = sdxDepositTopic;
    this.securityServicesClient = securityServicesClient;

    this.kafkaTemplate = kafkaTemplate;

    this.rsuDepositor = rsuDepositor;
    this.dataSigningEnabledSDW = securityServicesProperties.getIsSdwSigningEnabled();
    this.dataSigningEnabledRSU = securityServicesProperties.getIsRsuSigningEnabled();

    this.odeTimJsonTopology = odeTimJsonTopology;
    this.mapper = mapper;
  }

  /**
   * Listens for messages from the specified Kafka topic and processes them.
   *
   * <p>Cases:
   * - CASE 1: no SDW in metadata (SNMP deposit only) - sign MF - send to RSU - CASE 2: SDW in
   * metadata but no ASD in body (send back for another encoding) - sign MF - send to RSU - craft
   * ASD object - publish back to encoder stream - CASE 3: If SDW in metadata and ASD in body
   * (double encoding complete) - send to SDX
   *
   * </p>
   *
   * @param consumerRecord The Kafka consumer record containing the key and value of the consumed
   *                       message.
   */
  @KafkaListener(id = "Asn1EncodedDataRouter", topics = "${ode.kafka.topics.asn1.encoder-output}")
  public void listen(ConsumerRecord<String, String> consumerRecord)
      throws XmlUtilsException, JsonProcessingException, Asn1EncodedDataRouterException {
    JSONObject consumedObj = XmlUtils.toJSONObject(consumerRecord.value())
        .getJSONObject(OdeAsn1Data.class.getSimpleName());

    JSONObject metadata = consumedObj.getJSONObject(AppContext.METADATA_STRING);

    if (!metadata.has(TimTransmogrifier.REQUEST_STRING)) {
      throw new Asn1EncodedDataRouterException(
          String.format("Invalid or missing '%s' object in the encoder response. Unable to process record with key '%s'",
              TimTransmogrifier.REQUEST_STRING,
              consumerRecord.key())
      );
    }

    ServiceRequest request = getServicerequest(consumedObj);

    JSONObject payloadData = consumedObj.getJSONObject(AppContext.PAYLOAD_STRING).getJSONObject(AppContext.DATA_STRING);
    JSONObject metadataJson = consumedObj.getJSONObject(AppContext.METADATA_STRING);

    if (!payloadData.has(ADVISORY_SITUATION_DATA_STRING)) {
      processUnsignedMessage(request, metadataJson, payloadData);
    } else if (dataSigningEnabledSDW && request.getSdw() != null) {
      processSignedMessage(request, payloadData);
    } else {
      processEncodedTimUnsigned(request, metadataJson, payloadData);
    }
  }


  /**
   * Gets the service request based on the consumed JSONObject.
   *
   * @param consumedObj The object to retrieve the service request for
   *
   * @return The service request
   */
  private ServiceRequest getServicerequest(JSONObject consumedObj) throws JsonProcessingException {
    String serviceRequestJson = consumedObj.getJSONObject(AppContext.METADATA_STRING)
        .getJSONObject(TimTransmogrifier.REQUEST_STRING).toString();
    log.debug("ServiceRequest: {}", serviceRequestJson);
    return mapper.readValue(serviceRequestJson, ServiceRequest.class);
  }

  private void processSignedMessage(ServiceRequest request, JSONObject dataObj) {

    // Case 3: We have an ASD with signed MessageFrame
    JSONObject asdObj = dataObj.getJSONObject(ADVISORY_SITUATION_DATA_STRING);

    JSONObject deposit = new JSONObject();
    deposit.put("estimatedRemovalDate", request.getSdw().getEstimatedRemovalDate());
    deposit.put("encodedMsg", asdObj.getString(BYTES));
    kafkaTemplate.send(this.sdxDepositTopic, deposit.toString());
  }

  private void processUnsignedMessage(ServiceRequest request,
                                      JSONObject metadataJson,
                                      JSONObject payloadJson) {
    log.info("Processing unsigned message.");
    JSONObject messageFrameJson = payloadJson.getJSONObject(MESSAGE_FRAME);
    var hexEncodedTimBytes = messageFrameJson.getString(BYTES);

    // Case 1: SNMP-deposit
    if (dataSigningEnabledRSU && (request.getSdw() != null || request.getRsus() != null)) {
      var signedTimWithExpiration = signTimWithExpiration(hexEncodedTimBytes, metadataJson);
      kafkaTemplate.send(jsonTopics.getTimCertExpiration(), signedTimWithExpiration);
    }

    log.debug("Encoded message - phase 1: {}", hexEncodedTimBytes);
    var encodedTimWithoutHeaders = stripHeader(hexEncodedTimBytes);

    if (null != request.getSnmp() && null != request.getRsus()) {
      log.info("Sending message to RSUs...");
      sendToRsus(request, encodedTimWithoutHeaders);
    }

    depositToFilteredTopic(metadataJson, encodedTimWithoutHeaders);
    if (request.getSdw() != null) {
      log.debug("Publishing message for round 2 encoding");
      String asdPackagedTim = packageSignedTimIntoAsd(request, encodedTimWithoutHeaders);

      kafkaTemplate.send(asn1CoderTopics.getEncoderInput(), asdPackagedTim);
    }
  }

  private void processEncodedTimUnsigned(ServiceRequest request, JSONObject metadataJson, JSONObject payloadJson) {
    log.debug("Unsigned ASD received. Depositing it to SDW.");

    if (null != request.getSdw()) {
      var asdObj = payloadJson.getJSONObject(ADVISORY_SITUATION_DATA_STRING);
      if (null != asdObj) {
        depositToSdx(request, asdObj.getString(BYTES));
      } else {
        log.error("ASN.1 Encoder did not return ASD encoding {}", payloadJson);
      }
    }

    if (payloadJson.has(MESSAGE_FRAME)) {
      JSONObject mfObj = payloadJson.getJSONObject(MESSAGE_FRAME);
      String encodedTim = mfObj.getString(BYTES);

      depositToFilteredTopic(metadataJson, encodedTim);

      var encodedTimWithoutHeader = stripHeader(encodedTim);
      log.debug("Encoded message - phase 2: {}", encodedTimWithoutHeader);

      // only send message to rsu if snmp, rsus, and message frame fields are present
      if (null != request.getSnmp() && null != request.getRsus()) {
        sendToRsus(request, encodedTimWithoutHeader);
      }
    }
  }

  private void depositToSdx(ServiceRequest request, String asdBytes) {
    try {
      JSONObject deposit = new JSONObject();
      deposit.put("estimatedRemovalDate", request.getSdw().getEstimatedRemovalDate());
      deposit.put("encodedMsg", asdBytes);
      kafkaTemplate.send(this.sdxDepositTopic, deposit.toString());
      log.info("SDX deposit successful.");
    } catch (Exception e) {
      log.error(ERROR_ON_SDX_DEPOSIT, e);
    }
  }

  private String signTimWithExpiration(String encodedTIM, JSONObject metadataJson) {
    log.debug("Signing encoded TIM message...");
    String base64EncodedTim = CodecUtils.toBase64(
        CodecUtils.fromHex(encodedTIM));

    // get max duration time and convert from minutes to milliseconds (unsigned
    // integer valid 0 to 2^32-1 in units of
    // milliseconds.) from metadata
    int maxDurationTime = Integer.parseInt(metadataJson.get("maxDurationTime").toString())
        * 60 * 1000;
    String packetId = metadataJson.getString("odePacketID");
    String timStartDateTime = metadataJson.getString("odeTimStartDateTime");
    log.debug("SENDING: {}", base64EncodedTim);
    String signedResponse = securityServicesClient.signMessage(base64EncodedTim, maxDurationTime);

    JSONObject timWithExpiration = new JSONObject();
    timWithExpiration.put("packetID", packetId);
    timWithExpiration.put("startDateTime", timStartDateTime);

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    setExpiryDate(signedResponse, timWithExpiration, dateFormat);
    setRequiredExpiryDate(dateFormat, timStartDateTime, maxDurationTime, timWithExpiration);
    return timWithExpiration.toString();
  }

  /**
   * Constructs an XML representation of an Advisory Situation Data (ASD) message containing a
   * signed Traveler Information Message (TIM). Processes the provided service request and signed
   * message to create and structure the ASD before converting it to an XML string output.
   *
   * @param request   the service request object containing meta information, service region,
   *                  delivery time, and other necessary data for ASD creation.
   * @param signedMsg the signed Traveler Information Message (TIM) to be included in the ASD.
   *
   * @return a String containing the fully crafted ASD message in XML format. Returns null if the
   *     message could not be constructed due to exceptions.
   */
  private String packageSignedTimIntoAsd(ServiceRequest request, String signedMsg) {

    SDW sdw = request.getSdw();
    SNMP snmp = request.getSnmp();
    DdsAdvisorySituationData asd;

    byte sendToRsu =
        request.getRsus() != null ? DdsAdvisorySituationData.RSU : DdsAdvisorySituationData.NONE;
    byte distroType = (byte) (DdsAdvisorySituationData.IP | sendToRsu);

    String outputXml = null;
    try {
      if (null != snmp) {
        asd = new DdsAdvisorySituationData()
            .setAsdmDetails(snmp.getDeliverystart(), snmp.getDeliverystop(), distroType, null)
            .setServiceRegion(GeoRegionBuilder.ddsGeoRegion(sdw.getServiceRegion()))
            .setTimeToLive(sdw.getTtl())
            .setGroupID(sdw.getGroupID()).setRecordID(sdw.getRecordId());
      } else {
        asd = new DdsAdvisorySituationData()
            .setAsdmDetails(sdw.getDeliverystart(), sdw.getDeliverystop(), distroType, null)
            .setServiceRegion(GeoRegionBuilder.ddsGeoRegion(sdw.getServiceRegion()))
            .setTimeToLive(sdw.getTtl())
            .setGroupID(sdw.getGroupID()).setRecordID(sdw.getRecordId());
      }

      ObjectNode dataBodyObj = JsonUtils.newNode();
      ObjectNode asdObj = JsonUtils.toObjectNode(asd.toJson());
      ObjectNode admDetailsObj = (ObjectNode) asdObj.findValue("asdmDetails");
      admDetailsObj.remove("advisoryMessage");
      admDetailsObj.put("advisoryMessage", signedMsg);

      dataBodyObj.set(ADVISORY_SITUATION_DATA_STRING, asdObj);

      OdeMsgPayload payload = new OdeAsdPayload(asd);

      ObjectNode payloadObj = JsonUtils.toObjectNode(payload.toJson());
      payloadObj.set(AppContext.DATA_STRING, dataBodyObj);

      OdeMsgMetadata metadata = new OdeMsgMetadata(payload);
      ObjectNode metaObject = JsonUtils.toObjectNode(metadata.toJson());

      ObjectNode requestObj = JsonUtils.toObjectNode(JsonUtils.toJson(request, false));

      requestObj.remove("tim");

      metaObject.set("request", requestObj);

      ArrayNode encodings = buildEncodings();
      ObjectNode enc =
          XmlUtils.createEmbeddedJsonArrayForXmlConversion(AppContext.ENCODINGS_STRING,
              encodings);
      metaObject.set(AppContext.ENCODINGS_STRING, enc);

      ObjectNode message = JsonUtils.newNode();
      message.set(AppContext.METADATA_STRING, metaObject);
      message.set(AppContext.PAYLOAD_STRING, payloadObj);

      ObjectNode root = JsonUtils.newNode();
      root.set(AppContext.ODE_ASN1_DATA, message);

      outputXml = XmlUtils.toXmlStatic(root);

      // remove the surrounding <ObjectNode></ObjectNode>
      outputXml = outputXml.replace("<ObjectNode>", "");
      outputXml = outputXml.replace("</ObjectNode>", "");

    } catch (ParseException | JsonUtilsException | XmlUtilsException e) {
      log.error("Parsing exception thrown while populating ASD structure: ", e);
    }

    log.debug("Fully crafted ASD to be encoded: {}", outputXml);

    return outputXml;
  }

  private ArrayNode buildEncodings() throws JsonUtilsException {
    ArrayNode encodings = JsonUtils.newArrayNode();
    encodings.add(TimTransmogrifier.buildEncodingNode(ADVISORY_SITUATION_DATA_STRING,
        ADVISORY_SITUATION_DATA_STRING,
        EncodingRule.UPER));
    return encodings;
  }

  private void sendToRsus(ServiceRequest request, String encodedMsg) {
    rsuDepositor.deposit(request, encodedMsg);
  }

  private static void setRequiredExpiryDate(SimpleDateFormat dateFormat, String timStartDateTime,
                                            int maxDurationTime, JSONObject timWithExpiration) {
    try {
      Date timTimestamp = dateFormat.parse(timStartDateTime);
      Date requiredExpirationDate = new Date();
      requiredExpirationDate.setTime(timTimestamp.getTime() + maxDurationTime);
      timWithExpiration.put("requiredExpirationDate", dateFormat.format(requiredExpirationDate));
    } catch (Exception e) {
      log.error("Unable to parse requiredExpirationDate. Setting requiredExpirationDate to 'null'", e);
      timWithExpiration.put("requiredExpirationDate", "null");
    }
  }

  private static void setExpiryDate(String signedResponse,
                                    JSONObject timWithExpiration,
                                    SimpleDateFormat dateFormat) {
    try {
      JSONObject jsonResult = JsonUtils.toJSONObject(signedResponse).getJSONObject("result");
      // messageExpiry uses unit of seconds
      long messageExpiry = Long.parseLong(jsonResult.getString("message-expiry"));
      timWithExpiration.put("expirationDate", dateFormat.format(new Date(messageExpiry * 1000)));
    } catch (Exception e) {
      log.error("Unable to get expiration date from signed messages response. Setting expirationData to 'null'", e);
      timWithExpiration.put("expirationDate", "null");
    }
  }

  /**
   * Strips header from unsigned message (all bytes before 001F hex value).
   */
  private String stripHeader(String encodedUnsignedTim) {
    // find 001F hex value
    int index = encodedUnsignedTim.indexOf(SupportedMessageType.TIM.getStartFlag());
    if (index == -1) {
      log.warn("No '001F' hex value found in encoded message");
      return encodedUnsignedTim;
    }
    // strip everything before 001F
    return encodedUnsignedTim.substring(index);
  }

  private void depositToFilteredTopic(JSONObject metadataObj, String hexEncodedTim) {
    String generatedBy = metadataObj.getString("recordGeneratedBy");
    String streamId = metadataObj.getJSONObject("serialId").getString("streamId");
    if (!generatedBy.equalsIgnoreCase("TMC")) {
      log.debug("Not a TMC-generated TIM. Skipping deposit to TMC-filtered topic.");
      return;
    }

    String timString = odeTimJsonTopology.query(streamId);
    if (timString != null) {
      // Set ASN1 data in TIM metadata
      JSONObject timJSON = new JSONObject(timString);
      JSONObject metadataJSON = timJSON.getJSONObject("metadata");
      metadataJSON.put("asn1", hexEncodedTim);
      timJSON.put("metadata", metadataJSON);

      // Send the message w/ asn1 data to the TMC-filtered topic
      kafkaTemplate.send(jsonTopics.getTimTmcFiltered(), timJSON.toString());
    } else {
      log.debug("TIM not found in k-table. Skipping deposit to TMC-filtered topic.");
    }
  }
}
