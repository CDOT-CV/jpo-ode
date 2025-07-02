/*******************************************************************************
 * Copyright 2018 572682.
 *
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * </p>
 *
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * </p>
 *
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 * </p>
 ******************************************************************************/

package us.dot.its.jpo.ode.traveler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import us.dot.its.jpo.ode.coder.OdeMessageFrameDataCreatorHelper;
import us.dot.its.jpo.ode.kafka.topics.Asn1CoderTopics;
import us.dot.its.jpo.ode.kafka.topics.JsonTopics;
import us.dot.its.jpo.ode.model.OdeMessageFrameData;
import us.dot.its.jpo.ode.model.OdeMessageFrameMetadata;
import us.dot.its.jpo.ode.model.OdeMsgMetadata.GeneratedBy;
import us.dot.its.jpo.ode.model.OdeTravelerInputData;
import us.dot.its.jpo.ode.model.SerialId;
import us.dot.its.jpo.ode.plugin.ServiceRequest;
import us.dot.its.jpo.ode.plugin.ServiceRequest.OdeInternal;
import us.dot.its.jpo.ode.plugin.ServiceRequest.OdeInternal.RequestVerb;
import us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage;
import us.dot.its.jpo.ode.plugin.j2735.OdeTravelerInformationMessage.DataFrame;
import us.dot.its.jpo.ode.plugin.j2735.builders.TravelerMessageFromHumanToAsnConverter;
import us.dot.its.jpo.ode.security.SecurityServicesProperties;
import us.dot.its.jpo.ode.util.DateTimeUtils;
import us.dot.its.jpo.ode.util.JsonUtils;
import us.dot.its.jpo.ode.util.JsonUtils.JsonUtilsException;
import us.dot.its.jpo.ode.util.XmlUtils;

/**
 * The REST controller for handling TIM creation requests.
 */
@RestController
@Slf4j
public class TimDepositController {

  private static final TimIngestTracker INGEST_MONITOR = TimIngestTracker.getInstance();

  private static final String ERRSTR = "error";
  private static final String WARNING = "warning";
  private static final String SUCCESS = "success";

  private final Asn1CoderTopics asn1CoderTopics;
  private final JsonTopics jsonTopics;
  private final XmlMapper simpleXmlMapper;

  private final KafkaTemplate<String, String> kafkaTemplate;

  /**
   * Unique exception for the TimDepositController to handle error state responses to the client.
   */
  public static class TimDepositControllerException extends Exception {

    private static final long serialVersionUID = 1L;

    public TimDepositControllerException(String errMsg) {
      super(errMsg);
    }

  }

  /**
   * Spring Autowired constructor for the REST controller to properly initialize.
   */
  @Autowired
  public TimDepositController(Asn1CoderTopics asn1CoderTopics, JsonTopics jsonTopics,
      TimIngestTrackerProperties ingestTrackerProperties,
      SecurityServicesProperties securityServicesProperties,
      KafkaTemplate<String, String> kafkaTemplate,
      XmlMapper simpleXmlMapper) {
    super();

    this.asn1CoderTopics = asn1CoderTopics;
    this.jsonTopics = jsonTopics;
    this.simpleXmlMapper = simpleXmlMapper;

    this.kafkaTemplate = kafkaTemplate;

    // start the TIM ingest monitoring service if enabled
    if (ingestTrackerProperties.isTrackingEnabled()) {
      log.info("TIM ingest monitoring enabled.");

      ScheduledExecutorService scheduledExecutorService =
          Executors.newSingleThreadScheduledExecutor();

      scheduledExecutorService.scheduleAtFixedRate(
          new TimIngestWatcher(ingestTrackerProperties.getInterval()),
          ingestTrackerProperties.getInterval(), ingestTrackerProperties.getInterval(),
          java.util.concurrent.TimeUnit.SECONDS);
    } else {
      log.info("TIM ingest monitoring disabled.");
    }
  }

  /**
   * Send a TIM with the appropriate deposit type, ODE.PUT or ODE.POST.
   *
   * @param jsonString The value of the JSON message
   * @param verb The HTTP verb being requested
   *
   * @return The request completion status
   * @throws JsonProcessingException if there is an error processing the JSON
   */
  public synchronized ResponseEntity<String> depositTim(String jsonString, RequestVerb verb)
      throws JsonProcessingException {

    if (null == jsonString || jsonString.isEmpty()) {
      String errMsg = "Empty request.";
      log.error(errMsg);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(JsonUtils.jsonKeyValue(ERRSTR, errMsg));
    }

    OdeTravelerInputData odeTID;
    ServiceRequest request;
    try {
      // Convert JSON to POJO
      odeTID = (OdeTravelerInputData) JsonUtils.jacksonFromJson(jsonString,
          OdeTravelerInputData.class, true);
      if (odeTID == null) {
        String errMsg = "Malformed or non-compliant JSON syntax.";
        log.error(errMsg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(JsonUtils.jsonKeyValue(ERRSTR, errMsg));
      }

      request = odeTID.getRequest();
      if (request == null) {
        throw new TimDepositControllerException("Request element is required as of version 3.");
      }

      if (request.getOde() == null) {
        request.setOde(new OdeInternal());
      }

      request.getOde().setVerb(verb);

    } catch (TimDepositControllerException e) {
      String errMsg = "Missing or invalid argument: " + e.getMessage();
      log.error(errMsg, e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(JsonUtils.jsonKeyValue(ERRSTR, errMsg));
    } catch (JsonUtilsException e) {
      String errMsg = "Malformed or non-compliant JSON syntax.";
      log.error(errMsg, e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(JsonUtils.jsonKeyValue(ERRSTR, errMsg));
    }

    // Short circuit
    // If the TIM has no RSU/SNMP or SDW structures, return a warning
    if ((request.getRsus() == null || request.getSnmp() == null) && request.getSdw() == null) {
      String warningMsg = "Warning: TIM contains no RSU, SNMP, or SDW fields."
          + " Message only published to broadcast streams.";
      log.warn(warningMsg);
      return ResponseEntity.status(HttpStatus.OK).body(JsonUtils.jsonKeyValue(WARNING, warningMsg));
    }

    // Build the TIM metadata and deposit payload
    OdeTravelerInformationMessage tim = odeTID.getTim();
    OdeMessageFrameMetadata timMetadata = new OdeMessageFrameMetadata();

    // Configure the metadata
    timMetadata.setRequest(request);
    timMetadata.setOdePacketID(tim.getPacketID());
    // Calculate maxDurationTime and latestStartDateTime
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    if (null != tim.getDataframes() && tim.getDataframes().length > 0) {
      int maxDurationTime = 0;
      Date latestStartDateTime = null;
      for (DataFrame dataFrameItem : tim.getDataframes()) {
        maxDurationTime = Math.max(maxDurationTime, dataFrameItem.getDurationTime());
        try {
          latestStartDateTime = (latestStartDateTime == null || (latestStartDateTime != null
              && latestStartDateTime.before(dateFormat.parse(dataFrameItem.getStartDateTime())))
                  ? dateFormat.parse(dataFrameItem.getStartDateTime())
                  : latestStartDateTime);
        } catch (ParseException e) {
          log.error("Invalid dateTime parse: ", e);
        }
      }
      timMetadata.setMaxDurationTime(maxDurationTime);
      timMetadata.setOdeTimStartDateTime(dateFormat.format(latestStartDateTime));
    }
    timMetadata.setRecordGeneratedBy(GeneratedBy.TMC);
    // Determine where the message was generated from
    try {
      timMetadata.setRecordGeneratedAt(
          DateTimeUtils.isoDateTime(DateTimeUtils.isoDateTime(tim.getTimeStamp())));
    } catch (DateTimeParseException e) {
      String errMsg = "Invalid timestamp in tim record: " + tim.getTimeStamp();
      log.error(errMsg, e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(JsonUtils.jsonKeyValue(ERRSTR, errMsg));
    }
    // Set a unique serial ID for the ODE TIM message
    SerialId serialIdJ2735 = new SerialId();
    timMetadata.setSerialId(serialIdJ2735);

    // Craft ASN-encodable TIM
    ObjectNode encodableTid;
    try {
      encodableTid = JsonUtils.toObjectNode(odeTID.toJson());
      TravelerMessageFromHumanToAsnConverter.convertTravelerInputDataToEncodableTim(encodableTid);

      log.debug("Encodable Traveler Information Data: {}", encodableTid);

    } catch (JsonUtilsException e) {
      String errMsg = "Error converting to encodable TravelerInputData.";
      log.error(errMsg, e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(JsonUtils.jsonKeyValue(ERRSTR, errMsg));
    } catch (TravelerMessageFromHumanToAsnConverter.NoncompliantFieldsException e) {
      String errMsg = "Non-compliant fields in TIM: " + e.getMessage();
      log.error(errMsg, e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(JsonUtils.jsonKeyValue(ERRSTR, errMsg));
    } catch (TravelerMessageFromHumanToAsnConverter.InvalidNodeLatLonOffsetException e) {
      String errMsg = "Invalid node lat/lon offset in TIM: " + e.getMessage();
      log.error(errMsg);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(JsonUtils.jsonKeyValue(ERRSTR, errMsg));
    }

    // Publish the resulting ASN XML and JSON to the appropriate topics
    try {
      String xmlMsg;
      xmlMsg = TimTransmogrifier.convertToXml(null, encodableTid, timMetadata, serialIdJ2735);
      log.debug("XML representation: {}", xmlMsg);

      // Convert XML into ODE TIM JSON object and obfuscate RSU password
      OdeMessageFrameData odeTimMessageFrameData = OdeMessageFrameDataCreatorHelper
          .createOdeMessageFrameData(xmlMsg, simpleXmlMapper);

      // Create the TIM string and obfuscate the RSU password
      String j2735Tim = JsonUtils.toJson(odeTimMessageFrameData, false);
      String obfuscatedJ2735Tim = TimTransmogrifier.obfuscateRsuPassword(j2735Tim);

      // Publish TIM JSON to the OdeTimJson topic for the TIM topology KTable
      kafkaTemplate.send(jsonTopics.getTim(), serialIdJ2735.getStreamId(), obfuscatedJ2735Tim);
      // Publish TIM XML to the Asn1EncoderInput topic to be encoded by the ASN.1 Encoder module
      kafkaTemplate.send(asn1CoderTopics.getEncoderInput(), serialIdJ2735.getStreamId(), xmlMsg);
    } catch (JsonUtils.JsonUtilsException | XmlUtils.XmlUtilsException e) {
      String errMsg = "Error sending data to ASN.1 Encoder module: " + e.getMessage();
      log.error(errMsg, e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(JsonUtils.jsonKeyValue(ERRSTR, errMsg));
    }

    INGEST_MONITOR.incrementTotalMessagesReceived();
    return ResponseEntity.status(HttpStatus.OK).body(JsonUtils.jsonKeyValue(SUCCESS, "true"));
  }

  /**
   * Update an already-deposited TIM.
   *
   * @param jsonString TIM in JSON
   *
   * @return list of success/failures
   * @throws JsonProcessingException if there is an error processing the JSON
   */
  @PutMapping(value = "/tim", produces = "application/json")
  @CrossOrigin
  public ResponseEntity<String> putTim(@RequestBody String jsonString)
      throws JsonProcessingException {

    return depositTim(jsonString, OdeInternal.RequestVerb.PUT);
  }

  /**
   * Deposit a new TIM.
   *
   * @param jsonString TIM in JSON
   *
   * @return list of success/failures
   * @throws JsonProcessingException if there is an error processing the JSON
   */
  @PostMapping(value = "/tim", produces = "application/json")
  @CrossOrigin
  public ResponseEntity<String> postTim(@RequestBody String jsonString)
      throws JsonProcessingException {

    return depositTim(jsonString, OdeInternal.RequestVerb.POST);
  }

}
