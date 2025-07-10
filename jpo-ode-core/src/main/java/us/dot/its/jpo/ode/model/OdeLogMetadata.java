/*******************************************************************************
 * . Copyright 2018 572682
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

package us.dot.its.jpo.ode.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * OdeLogMetadata is a class that extends OdeMsgMetadata and represents the metadata fields in a
 * processed ODE message.
 */
@JsonPropertyOrder({"logFileName", "recordType", "securityResultCode", "receivedMessageDetails",
    "longitude", "elevation", "speed", "heading", "rxSource", "encodings", "payloadType",
    "serialId", "odeReceivedAt", "schemaVersion", "maxDurationTime", "recordGeneratedAt",
    "recordGeneratedBy", "sanitized"})
public class OdeLogMetadata extends OdeMsgMetadata {

  private static final long serialVersionUID = -8601265839394150140L;

  /**
   * Enum representing the possible record types for log metadata.
   */
  public enum RecordType {
    bsmLogDuringEvent, rxMsg, dnMsg, bsmTx, driverAlert, mapTx, spatTx, ssmTx, srmTx, timMsg, psmTx, sdsmTx, unsupported
  }

  /**
   * Enum representing the possible security result codes for log metadata.
   */
  public enum SecurityResultCode {
    success,
    unknown,
    inconsistentInputParameters,
    spduParsingInvalidInput,
    spduParsingUnsupportedCriticalInformationField,
    spduParsingCertificateNotFound,
    spduParsingGenerationTimeNotAvailable,
    spduParsingGenerationLocationNotAvailable,
    spduCertificateChainNotEnoughInformationToConstructChain,
    spduCertificateChainChainEndedAtUntrustedRoot,
    spduCertificateChainChainWasTooLongForImplementation,
    spduCertificateChainCertificateRevoked,
    spduCertificateChainOverdueCRL,
    spduCertificateChainInconsistentExpiryTimes,
    spduCertificateChainInconsistentStartTimes,
    spduCertificateChainInconsistentChainPermissions,
    spduCryptoVerificationFailure,
    spduConsistencyFutureCertificateAtGenerationTime,
    spduConsistencyExpiredCertificateAtGenerationTime,
    spduConsistencyExpiryDateTooEarly,
    spduConsistencyExpiryDateTooLate,
    spduConsistencyGenerationLocationOutsideValidityRegion,
    spduConsistencyNoGenerationLocation,
    spduConsistencyUnauthorizedPSID,
    spduInternalConsistencyExpiryTimeBeforeGenerationTime,
    spduInternalConsistencyextDataHashDoesntMatch,
    spduInternalConsistencynoExtDataHashProvided,
    spduInternalConsistencynoExtDataHashPresent,
    spduLocalConsistencyPSIDsDontMatch,
    spduLocalConsistencyChainWasTooLongForSDEE,
    spduRelevanceGenerationTimeTooFarInPast,
    spduRelevanceGenerationTimeTooFarInFuture,
    spduRelevanceExpiryTimeInPast,
    spduRelevanceGenerationLocationTooDistant,
    spduRelevanceReplayedSpdu,
    spduCertificateExpired
  }

  private String logFileName;
  private RecordType recordType;
  private SecurityResultCode securityResultCode;
  private ReceivedMessageDetails receivedMessageDetails;
  @JsonDeserialize(using = EncodingsDeserializer.class)
  private List<Asn1Encoding> encodings;

  public OdeLogMetadata(OdeMsgPayload payload) {
    super(payload);
  }

  public OdeLogMetadata() {
    super();
  }

  public OdeLogMetadata(String payloadType, SerialId serialId, String receivedAt) {
    super(payloadType, serialId, receivedAt);
  }

  /**
   * Calculates and sets the recordGeneratedBy field based on the receivedMessageDetails' rxSource.
   */
  public void calculateGeneratedBy() {
    ReceivedMessageDetails receivedMessageDetails = getReceivedMessageDetails();
    if (receivedMessageDetails != null) {
      if (receivedMessageDetails.getRxSource() != null) {
        switch (receivedMessageDetails.getRxSource()) {
          case RSU:
            setRecordGeneratedBy(GeneratedBy.RSU);
            break;
          case RV:
          case NA:
            setRecordGeneratedBy(GeneratedBy.OBU);
            break;
          case SAT:
            setRecordGeneratedBy(GeneratedBy.TMC_VIA_SAT);
            break;
          case SNMP:
            setRecordGeneratedBy(GeneratedBy.TMC_VIA_SNMP);
            break;
          default:
            setRecordGeneratedBy(GeneratedBy.UNKNOWN);
            break;
        }
      }
    } else {
      setRecordGeneratedBy(GeneratedBy.OBU);
    }
  }

  public String getLogFileName() {
    return logFileName;
  }

  public void setLogFileName(String logFileName) {
    this.logFileName = logFileName;
  }

  public RecordType getRecordType() {
    return recordType;
  }

  public void setRecordType(RecordType recordType) {
    this.recordType = recordType;
  }

  public SecurityResultCode getSecurityResultCode() {
    return securityResultCode;
  }

  public void setSecurityResultCode(SecurityResultCode securityResultCode) {
    this.securityResultCode = securityResultCode;
  }

  public ReceivedMessageDetails getReceivedMessageDetails() {
    return receivedMessageDetails;
  }

  public void setReceivedMessageDetails(ReceivedMessageDetails receivedMessageDetails) {
    this.receivedMessageDetails = receivedMessageDetails;
  }

  public List<Asn1Encoding> getEncodings() {
    return encodings;
  }

  public void setEncodings(List<Asn1Encoding> encodings) {
    this.encodings = encodings;
  }

  /**
   * Adds an Asn1Encoding object to the list of encodings.
   *
   * @param encoding the Asn1Encoding object to add
   * @return this OdeLogMetadata instance
   */
  public OdeLogMetadata addEncoding(Asn1Encoding encoding) {
    if (encodings == null) {
      encodings = new ArrayList<Asn1Encoding>();
    }
    encodings.add(encoding);
    return this;
  }

  /**
   * Custom deserializer for the list of Asn1Encoding objects used in OdeLogMetadata.
   */
  public static class EncodingsDeserializer extends JsonDeserializer<List<Asn1Encoding>> {
    @Override
    public List<Asn1Encoding> deserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException {
      JsonNode node = p.getCodec().readTree(p);
      List<Asn1Encoding> result = new ArrayList<>();

      if (node != null && node.has("encodings")) {
        JsonNode encodingsNode = node.get("encodings");

        // Create an Asn1Encoding object manually from the nested structure
        Asn1Encoding encoding = new Asn1Encoding();
        if (encodingsNode.has("elementName")) {
          encoding.setElementName(encodingsNode.get("elementName").asText());
        }
        if (encodingsNode.has("elementType")) {
          encoding.setElementType(encodingsNode.get("elementType").asText());
        }
        if (encodingsNode.has("encodingRule")) {
          encoding.setEncodingRule(
              Asn1Encoding.EncodingRule.valueOf(encodingsNode.get("encodingRule").asText()));
        }

        result.add(encoding);
      }

      return result;
    }
  }
}
