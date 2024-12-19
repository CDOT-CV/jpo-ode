/*=============================================================================
 * Copyright 2018 572682
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package us.dot.its.jpo.ode.services.asn1;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.text.ParseException;
import lombok.extern.slf4j.Slf4j;
import us.dot.its.jpo.ode.context.AppContext;
import us.dot.its.jpo.ode.eventlog.EventLogger;
import us.dot.its.jpo.ode.model.Asn1Encoding.EncodingRule;
import us.dot.its.jpo.ode.model.OdeAsdPayload;
import us.dot.its.jpo.ode.model.OdeMsgMetadata;
import us.dot.its.jpo.ode.model.OdeMsgPayload;
import us.dot.its.jpo.ode.plugin.SNMP;
import us.dot.its.jpo.ode.plugin.ServiceRequest;
import us.dot.its.jpo.ode.plugin.SituationDataWarehouse.SDW;
import us.dot.its.jpo.ode.plugin.j2735.DdsAdvisorySituationData;
import us.dot.its.jpo.ode.plugin.j2735.builders.GeoRegionBuilder;
import us.dot.its.jpo.ode.rsu.RsuDepositor;
import us.dot.its.jpo.ode.traveler.TimTransmogrifier;
import us.dot.its.jpo.ode.util.JsonUtils;
import us.dot.its.jpo.ode.util.JsonUtils.JsonUtilsException;
import us.dot.its.jpo.ode.util.XmlUtils;
import us.dot.its.jpo.ode.util.XmlUtils.XmlUtilsException;

/**
 * Manages operations related to ASN.1 command processing and interaction with RSUs (Road Side
 * Units). Handles the preparation, packaging, and distribution of messages to RSUs using an
 * {@link RsuDepositor}.
 */
@Slf4j
public class Asn1CommandManager {

  public static final String ADVISORY_SITUATION_DATA_STRING = "AdvisorySituationData";

  private RsuDepositor rsuDepositor;

  /**
   * Constructs an instance of Asn1CommandManager and initializes the RSU depositor.
   *
   * @param rsuDepositor The RSU depositor instance to be used for managing ASN.1 commands. This
   *                     instance is started during initialization.
   */
  public Asn1CommandManager(RsuDepositor rsuDepositor) {
    try {
      this.rsuDepositor = rsuDepositor;
      this.rsuDepositor.start();
    } catch (Exception e) {
      String msg = "Error starting SDW depositor";
      EventLogger.logger.error(msg, e);
      log.error(msg, e);
    }
  }

  /**
   * Sends a message to the RSUs (Road Side Units) through the RSU depositor.
   *
   * @param request    the service request containing relevant details for the operation
   * @param encodedMsg the message to be sent, encoded in the desired format
   */
  public void sendToRsus(ServiceRequest request, String encodedMsg) {
    rsuDepositor.deposit(request, encodedMsg);
  }

  /**
   * Constructs an XML representation of an Advisory Situation Data (ASD) message containing a
   * signed Traveler Information Message (TIM). Processes the provided service request and signed
   * message to create and structure the ASD before converting it to an XML string output.
   *
   * @param request   the service request object containing meta information, service region,
   *                  delivery time, and other necessary data for ASD creation.
   * @param signedMsg the signed Traveler Information Message (TIM) to be included in the ASD.
   * @return          a String containing the fully crafted ASD message in XML format. Returns null if the
   *                  message could not be constructed due to exceptions.
   */
  public String packageSignedTimIntoAsd(ServiceRequest request, String signedMsg) {

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
          XmlUtils.createEmbeddedJsonArrayForXmlConversion(AppContext.ENCODINGS_STRING, encodings);
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
}
