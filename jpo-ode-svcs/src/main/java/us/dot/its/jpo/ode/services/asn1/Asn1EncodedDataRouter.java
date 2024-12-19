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

package us.dot.its.jpo.ode.services.asn1;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import us.dot.its.jpo.ode.OdeTimJsonTopology;
import us.dot.its.jpo.ode.context.AppContext;
import us.dot.its.jpo.ode.eventlog.EventLogger;
import us.dot.its.jpo.ode.kafka.OdeKafkaProperties;
import us.dot.its.jpo.ode.kafka.topics.Asn1CoderTopics;
import us.dot.its.jpo.ode.kafka.topics.JsonTopics;
import us.dot.its.jpo.ode.model.OdeAsn1Data;
import us.dot.its.jpo.ode.plugin.ServiceRequest;
import us.dot.its.jpo.ode.security.ISecurityServicesClient;
import us.dot.its.jpo.ode.security.SecurityServicesProperties;
import us.dot.its.jpo.ode.services.asn1.Asn1CommandManager.Asn1CommandManagerException;
import us.dot.its.jpo.ode.traveler.TimTransmogrifier;
import us.dot.its.jpo.ode.util.CodecUtils;
import us.dot.its.jpo.ode.util.JsonUtils;
import us.dot.its.jpo.ode.util.JsonUtils.JsonUtilsException;
import us.dot.its.jpo.ode.util.XmlUtils;
import us.dot.its.jpo.ode.wrapper.AbstractSubscriberProcessor;
import us.dot.its.jpo.ode.wrapper.MessageProducer;

/**
 * The Asn1EncodedDataRouter is responsible for routing encoded TIM messages that are consumed from
 * the Kafka topic.Asn1EncoderOutput topic and decide whether to route to the SDX or an RSU.
 **/
@Slf4j
public class Asn1EncodedDataRouter extends AbstractSubscriberProcessor<String, String> {

  private static final String BYTES = "bytes";
  private static final String MESSAGE_FRAME = "MessageFrame";
  private static final String ERROR_ON_SDX_DEPOSIT = "Error on SDX deposit.";

  /**
   * Exception for Asn1EncodedDataRouter specific failures.
   */
  public static class Asn1EncodedDataRouterException extends Exception {

    private static final long serialVersionUID = 1L;

    public Asn1EncodedDataRouterException(String string) {
      super(string);
    }
  }

  private final Asn1CoderTopics asn1CoderTopics;
  private final JsonTopics jsonTopics;
  private final ISecurityServicesClient securityServicesClient;

  private final MessageProducer<String, String> stringMsgProducer;
  private final OdeTimJsonTopology odeTimJsonTopology;
  private final Asn1CommandManager asn1CommandManager;
  private final boolean dataSigningEnabledSDW;
  private final boolean dataSigningEnabledRSU;

  /**
   * Instantiates the Asn1EncodedDataRouter to actively consume from Kafka and route the encoded TIM
   * messages to the SDX and RSUs.
   *
   * @param odeKafkaProperties         The Kafka properties used to consume and produce to Kafka
   * @param asn1CoderTopics            The specified ASN1 Coder topics
   * @param jsonTopics                 The specified JSON topics to write to
   * @param securityServicesProperties The security services properties to use
   * @param securityServicesClient
   **/
  public Asn1EncodedDataRouter(OdeKafkaProperties odeKafkaProperties,
      Asn1CoderTopics asn1CoderTopics,
      JsonTopics jsonTopics,
      SecurityServicesProperties securityServicesProperties,
      OdeTimJsonTopology odeTimJsonTopology,
      Asn1CommandManager asn1CommandManager,
      ISecurityServicesClient securityServicesClient) {
    super();

    this.asn1CoderTopics = asn1CoderTopics;
    this.jsonTopics = jsonTopics;
    this.securityServicesClient = securityServicesClient;

    this.stringMsgProducer = MessageProducer.defaultStringMessageProducer(
        odeKafkaProperties.getBrokers(),
        odeKafkaProperties.getKafkaType(),
        odeKafkaProperties.getDisabledTopics());

    this.asn1CommandManager = asn1CommandManager;
    this.dataSigningEnabledSDW = securityServicesProperties.getIsSdwSigningEnabled();
    this.dataSigningEnabledRSU = securityServicesProperties.getIsRsuSigningEnabled();

    this.odeTimJsonTopology = odeTimJsonTopology;
  }

  @Override
  public Object process(String consumedData) {
    try {
      log.debug("Consumed: {}", consumedData);
      JSONObject consumedObj = XmlUtils.toJSONObject(consumedData).getJSONObject(
          OdeAsn1Data.class.getSimpleName());

      /*
       * When receiving the 'rsus' in xml, since there is only one 'rsu' and
       * there is no construct for array in xml, the rsus does not translate
       * to an array of 1 element. The following workaround, resolves this
       * issue.
       */
      JSONObject metadata = consumedObj.getJSONObject(AppContext.METADATA_STRING);

      if (metadata.has(TimTransmogrifier.REQUEST_STRING)) {
        JSONObject request = metadata.getJSONObject(TimTransmogrifier.REQUEST_STRING);
        if (request.has(TimTransmogrifier.RSUS_STRING)) {
          Object rsus = request.get(TimTransmogrifier.RSUS_STRING);
          if (rsus instanceof JSONObject) {
            JSONObject rsusIn = (JSONObject) request.get(TimTransmogrifier.RSUS_STRING);
            if (rsusIn.has(TimTransmogrifier.RSUS_STRING)) {
              Object rsu = rsusIn.get(TimTransmogrifier.RSUS_STRING);
              JSONArray rsusOut = new JSONArray();
              if (rsu instanceof JSONArray) {
                log.debug("Multiple RSUs exist in the request: {}", request);
                JSONArray rsusInArray = (JSONArray) rsu;
                for (int i = 0; i < rsusInArray.length(); i++) {
                  rsusOut.put(rsusInArray.get(i));
                }
                request.put(TimTransmogrifier.RSUS_STRING, rsusOut);
              } else if (rsu instanceof JSONObject) {
                log.debug("Single RSU exists in the request: {}", request);
                rsusOut.put(rsu);
                request.put(TimTransmogrifier.RSUS_STRING, rsusOut);
              } else {
                log.debug("No RSUs exist in the request: {}", request);
                request.remove(TimTransmogrifier.RSUS_STRING);
              }
            }
          }
        }

        // Convert JSON to POJO
        ServiceRequest servicerequest = getServicerequest(consumedObj);

        processEncodedTim(servicerequest, consumedObj);
      } else {
        throw new Asn1EncodedDataRouterException("Invalid or missing '"
            + TimTransmogrifier.REQUEST_STRING + "' object in the encoder response");
      }
    } catch (Exception e) {
      String msg = "Error in processing received message from ASN.1 Encoder module: "
          + consumedData;
      if (log.isDebugEnabled()) {
        // print error message and stack trace
        EventLogger.logger.error(msg, e);
        log.error(msg, e);
      } else {
        // print error message only
        EventLogger.logger.error(msg);
        log.error(msg);
      }
    }
    return null;
  }

  /**
   * Gets the service request based on the consumed JSONObject.
   *
   * @param consumedObj The object to retrieve the service request for
   * @return The service request
   */
  public ServiceRequest getServicerequest(JSONObject consumedObj) {
    String sr = consumedObj.getJSONObject(AppContext.METADATA_STRING).getJSONObject(
        TimTransmogrifier.REQUEST_STRING).toString();
    log.debug("ServiceRequest: {}", sr);

    // Convert JSON to POJO
    ServiceRequest serviceRequest = null;
    try {
      serviceRequest = (ServiceRequest) JsonUtils.fromJson(sr, ServiceRequest.class);

    } catch (Exception e) {
      String errMsg = "Malformed JSON.";
      EventLogger.logger.error(errMsg, e);
      log.error(errMsg, e);
    }

    return serviceRequest;
  }

  /**
   * Process the signed encoded TIM message.
   *
   * @param request     The service request
   * @param consumedObj The consumed JSON object
   */
  public void processEncodedTim(ServiceRequest request, JSONObject consumedObj) {

    JSONObject dataObj = consumedObj.getJSONObject(AppContext.PAYLOAD_STRING).getJSONObject(
        AppContext.DATA_STRING);
    JSONObject metadataObj = consumedObj.getJSONObject(AppContext.METADATA_STRING);

    // CASE 1: no SDW in metadata (SNMP deposit only)
    // - sign MF
    // - send to RSU
    // CASE 2: SDW in metadata but no ASD in body (send back for another
    // encoding)
    // - sign MF
    // - send to RSU
    // - craft ASD object
    // - publish back to encoder stream
    // CASE 3: If SDW in metadata and ASD in body (double encoding complete)
    // - send to SDX

    if (!dataObj.has(Asn1CommandManager.ADVISORY_SITUATION_DATA_STRING)) {
      processSNMPDepositOnly(request, consumedObj, dataObj, metadataObj);
    } else {
      // We have encoded ASD. It could be either UNSECURED or secured.
      if (dataSigningEnabledSDW && request.getSdw() != null) {
        processSignedMessage(request, dataObj);
      } else {
        processEncodedTimUnsecured(request, consumedObj);
      }
    }
  }

  private void processSignedMessage(ServiceRequest request, JSONObject dataObj) {
    log.debug("Signed message received. Depositing it to SDW.");
    // We have a ASD with signed MessageFrame
    // Case 3
    JSONObject asdObj = dataObj.getJSONObject(
        Asn1CommandManager.ADVISORY_SITUATION_DATA_STRING);
    try {
      JSONObject deposit = new JSONObject();
      deposit.put("estimatedRemovalDate", request.getSdw().getEstimatedRemovalDate());
      deposit.put("encodedMsg", asdObj.getString(BYTES));
      asn1CommandManager.depositToSdw(deposit.toString());
    } catch (JSONException | Asn1CommandManagerException e) {
      String msg = ERROR_ON_SDX_DEPOSIT;
      log.error(msg, e);
    }
  }

  private void processSNMPDepositOnly(ServiceRequest request, JSONObject consumedObj,
      JSONObject dataObj,
      JSONObject metadataObj) {
    log.debug("Unsigned message received");
    // We don't have ASD, therefore it must be just a MessageFrame that needs to be
    // signed
    // No support for unsecured MessageFrame only payload.
    // Cases 1 & 2
    // Sign and send to RSUs

    JSONObject mfObj = dataObj.getJSONObject(MESSAGE_FRAME);

    String hexEncodedTim = mfObj.getString(BYTES);
    log.debug("Encoded message - phase 1: {}", hexEncodedTim);
    // use Asnc1 library to decode the encoded tim returned from ASNC1; another
    // class two blockers: decode the tim and decode the message-sign

    // Case 1: SNMP-deposit
    if (dataSigningEnabledRSU && request.getRsus() != null) {
      hexEncodedTim = signTIMAndProduceToExpireTopic(hexEncodedTim, consumedObj);
    } else {
      // if header is present, strip it
      if (isHeaderPresent(hexEncodedTim)) {
        String header = hexEncodedTim.substring(0, hexEncodedTim.indexOf("001F") + 4);
        log.debug("Stripping header from unsigned message: {}", header);
        hexEncodedTim = stripHeader(hexEncodedTim);
        mfObj.remove(BYTES);
        mfObj.put(BYTES, hexEncodedTim);
        dataObj.remove(MESSAGE_FRAME);
        dataObj.put(MESSAGE_FRAME, mfObj);
        consumedObj.remove(AppContext.PAYLOAD_STRING);
        consumedObj.put(AppContext.PAYLOAD_STRING, dataObj);
      }
    }

    if (null != request.getSnmp() && null != request.getRsus() && null != hexEncodedTim) {
      log.info("Sending message to RSUs...");
      asn1CommandManager.sendToRsus(request, hexEncodedTim);
    }

    hexEncodedTim = mfObj.getString(BYTES);

    // Case 2: SDX-deposit
    if (dataSigningEnabledSDW && request.getSdw() != null) {
      hexEncodedTim = signTIMAndProduceToExpireTopic(hexEncodedTim, consumedObj);
    }

    // Deposit encoded & signed TIM to TMC-filtered topic if TMC-generated
    depositToFilteredTopic(metadataObj, hexEncodedTim);
    if (request.getSdw() != null) {
      // Case 2 only

      log.debug("Publishing message for round 2 encoding!");
      String xmlizedMessage = asn1CommandManager.packageSignedTimIntoAsd(request, hexEncodedTim);

      stringMsgProducer.send(asn1CoderTopics.getEncoderInput(), null, xmlizedMessage);
    }
  }

  /**
   * Process the unsigned encoded TIM message.
   *
   * @param request     The service request
   * @param consumedObj The consumed JSON object
   */
  public void processEncodedTimUnsecured(ServiceRequest request, JSONObject consumedObj) {
    log.debug("Unsigned ASD received. Depositing it to SDW.");
    // We have ASD with UNSECURED MessageFrame
    // Send TIMs and record results
    HashMap<String, String> responseList = new HashMap<>();
    JSONObject metadataObj = consumedObj.getJSONObject(AppContext.METADATA_STRING);

    JSONObject dataObj = consumedObj
        .getJSONObject(AppContext.PAYLOAD_STRING)
        .getJSONObject(AppContext.DATA_STRING);

    if (null != request.getSdw()) {
      JSONObject asdObj = null;
      if (dataObj.has(Asn1CommandManager.ADVISORY_SITUATION_DATA_STRING)) {
        asdObj = dataObj.getJSONObject(Asn1CommandManager.ADVISORY_SITUATION_DATA_STRING);
      } else {
        log.error("ASD structure present in metadata but not in JSONObject!");
      }

      if (null != asdObj) {
        String asdBytes = asdObj.getString(BYTES);

        try {
          JSONObject deposit = new JSONObject();
          deposit.put("estimatedRemovalDate", request.getSdw().getEstimatedRemovalDate());
          deposit.put("encodedMsg", asdBytes);
          asn1CommandManager.depositToSdw(deposit.toString());
          log.info("SDX deposit successful.");
        } catch (Exception e) {
          String msg = ERROR_ON_SDX_DEPOSIT;
          log.error(msg, e);
          EventLogger.logger.error(msg, e);
        }

      } else if (log.isErrorEnabled()) {
        // Added to avoid Sonar's "Invoke method(s) only conditionally." code smell
        String msg = "ASN.1 Encoder did not return ASD encoding {}";
        EventLogger.logger.error(msg, consumedObj);
        log.error(msg, consumedObj);
      }
    }

    if (dataObj.has(MESSAGE_FRAME)) {
      JSONObject mfObj = dataObj.getJSONObject(MESSAGE_FRAME);
      String encodedTim = mfObj.getString(BYTES);

      // Deposit encoded TIM to TMC-filtered topic if TMC-generated
      depositToFilteredTopic(metadataObj, encodedTim);

      // if header is present, strip it
      if (isHeaderPresent(encodedTim)) {
        String header = encodedTim.substring(0, encodedTim.indexOf("001F") + 4);
        log.debug("Stripping header from unsigned message: {}", header);
        encodedTim = stripHeader(encodedTim);
        mfObj.remove(BYTES);
        mfObj.put(BYTES, encodedTim);
        dataObj.remove(MESSAGE_FRAME);
        dataObj.put(MESSAGE_FRAME, mfObj);
        consumedObj.remove(AppContext.PAYLOAD_STRING);
        consumedObj.put(AppContext.PAYLOAD_STRING, dataObj);
      }

      log.debug("Encoded message - phase 2: {}", encodedTim);

      // only send message to rsu if snmp, rsus, and message frame fields are present
      if (null != request.getSnmp() && null != request.getRsus() && null != encodedTim) {
        log.debug("Encoded message phase 3: {}", encodedTim);
        asn1CommandManager.sendToRsus(request, encodedTim);
      }
    }

    log.info("TIM deposit response {}", responseList);
  }

  /**
   * Sign the encoded TIM message and write to Kafka with an expiration time.
   *
   * @param encodedTIM  The encoded TIM message to be signed
   * @param consumedObj The JSON object to be consumed
   * @return The String representation of the encodedTim payload
   */
  public String signTIMAndProduceToExpireTopic(String encodedTIM, JSONObject consumedObj) {
    log.debug("Sending message for signature! ");
    String base64EncodedTim = CodecUtils.toBase64(
        CodecUtils.fromHex(encodedTIM));
    JSONObject metadataObjs = consumedObj.getJSONObject(AppContext.METADATA_STRING);
    // get max duration time and convert from minutes to milliseconds (unsigned
    // integer valid 0 to 2^32-1 in units of
    // milliseconds.) from metadata
    int maxDurationTime = Integer.valueOf(metadataObjs.get("maxDurationTime").toString())
        * 60 * 1000;
    String timpacketID = metadataObjs.getString("odePacketID");
    String timStartDateTime = metadataObjs.getString("odeTimStartDateTime");
    log.debug("SENDING: {}", base64EncodedTim);
    String signedResponse = securityServicesClient.signMessage(base64EncodedTim, maxDurationTime);
    try {
      final String hexEncodedTim = CodecUtils.toHex(
          CodecUtils.fromBase64(
              JsonUtils.toJSONObject(signedResponse).getJSONObject("result")
                  .getString("message-signed")));

      JSONObject timWithExpiration = new JSONObject();
      timWithExpiration.put("packetID", timpacketID);
      timWithExpiration.put("startDateTime", timStartDateTime);

      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
      setExpiryDate(signedResponse, timWithExpiration, dateFormat);
      setRequiredExpiryDate(dateFormat, timStartDateTime, maxDurationTime, timWithExpiration);

      // publish to Tim expiration kafka
      stringMsgProducer.send(jsonTopics.getTimCertExpiration(), null,
          timWithExpiration.toString());

      return hexEncodedTim;

    } catch (JsonUtilsException e1) {
      log.error("Unable to parse signed message response ", e1);
    }
    return encodedTIM;
  }

  private static void setRequiredExpiryDate(SimpleDateFormat dateFormat, String timStartDateTime,
      int maxDurationTime, JSONObject timWithExpiration) {
    try {
      Date parsedtimTimeStamp = dateFormat.parse(timStartDateTime);
      Date requiredExpirationDate = new Date();
      requiredExpirationDate.setTime(parsedtimTimeStamp.getTime() + maxDurationTime);
      timWithExpiration.put("requiredExpirationDate", dateFormat.format(requiredExpirationDate));
    } catch (Exception e) {
      log.error("Unable to parse requiredExpirationDate ", e);
      timWithExpiration.put("requiredExpirationDate", "null");
    }
  }

  private static void setExpiryDate(String signedResponse, JSONObject timWithExpiration,
      SimpleDateFormat dateFormat) {
    try {
      JSONObject jsonResult = JsonUtils
          .toJSONObject((JsonUtils.toJSONObject(signedResponse).getString("result")));
      // messageExpiry uses unit of seconds
      long messageExpiry = Long.parseLong(jsonResult.getString("message-expiry"));
      timWithExpiration.put("expirationDate", dateFormat.format(new Date(messageExpiry * 1000)));
    } catch (Exception e) {
      log.error("Unable to get expiration date from signed messages response ", e);
      timWithExpiration.put("expirationDate", "null");
    }
  }

  /**
   * Checks if header is present in encoded message.
   */
  private boolean isHeaderPresent(String encodedTim) {
    return encodedTim.indexOf("001F") > 0;
  }

  /**
   * Strips header from unsigned message (all bytes before 001F hex value).
   */
  private String stripHeader(String encodedUnsignedTim) {
    String toReturn = "";
    // find 001F hex value
    int index = encodedUnsignedTim.indexOf("001F");
    if (index == -1) {
      log.warn("No '001F' hex value found in encoded message");
      return encodedUnsignedTim;
    }
    // strip everything before 001F
    toReturn = encodedUnsignedTim.substring(index);
    return toReturn;
  }

  private void depositToFilteredTopic(JSONObject metadataObj, String hexEncodedTim) {
    try {
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
        stringMsgProducer.send(jsonTopics.getTimTmcFiltered(), null, timJSON.toString());
      }
    } catch (JSONException e) {
      log.error("Error while fetching recordGeneratedBy field: {}", e.getMessage());
    } catch (Exception e) {
      log.error("Error while updating TIM: {}", e.getMessage());
    }
  }
}
