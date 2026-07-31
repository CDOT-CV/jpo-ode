package us.dot.its.jpo.ode.coder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.time.Instant;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import us.dot.its.jpo.asn.j2735.r2024.MessageFrame.MessageFrame;
import us.dot.its.jpo.ode.model.OdeMessageFrameData;
import us.dot.its.jpo.ode.model.OdeMessageFrameMetadata;
import us.dot.its.jpo.ode.model.OdeMessageFramePayload;
import us.dot.its.jpo.ode.model.RxSource;
import us.dot.its.jpo.ode.model.SignedDataMetadata;
import us.dot.its.jpo.ode.plugin.ServiceRequest;

/**
 * Helper class for creating OdeMessageFrameData objects from consumed data.
 */
@Slf4j
public class OdeMessageFrameDataCreatorHelper {

  private static final Instant IEEE_1609_2_EPOCH = Instant.parse("2004-01-01T00:00:00Z");

  private OdeMessageFrameDataCreatorHelper() {
    throw new UnsupportedOperationException("Utility class should not be instantiated");
  }

  /**
   * Creates an OdeMessageFrameData object from consumed XML data.
   *
   * @param consumedData The XML data string to be processed
   * @param simpleXmlMapper The XmlMapper for XML operations
   * @return OdeMessageFrameData object containing the processed data
   * @throws JsonProcessingException if there is an error processing the JSON data
   */
  public static OdeMessageFrameData createOdeMessageFrameData(String consumedData, 
      XmlMapper simpleXmlMapper) throws JsonProcessingException {
    // Parse the XML into a tree structure first
    JsonNode rootNode = simpleXmlMapper.readTree(consumedData);
    
    // Extract and deserialize metadata separately
    JsonNode metadataNode = rootNode.get("metadata");
    ServiceRequest request = null;
    if (metadataNode instanceof ObjectNode object) {
      if (object.has("request")) {
        JsonNode requestNode = object.get("request");
        // Check if "request" is present and not an empty object
        if (requestNode != null && requestNode.isObject() && requestNode.size() > 0) {
          String xmlBack = simpleXmlMapper.writeValueAsString(requestNode);
          request = simpleXmlMapper.readValue(xmlBack, ServiceRequest.class);
        }
        object.remove("request");
      }
    }
    SignedDataMetadata certMetadata = extractCertMetadata(metadataNode);
    OdeMessageFrameMetadata metadata = simpleXmlMapper.treeToValue(metadataNode, OdeMessageFrameMetadata.class);
    metadata.setRequest(request);
    metadata.setCertMetadata(certMetadata);
    // Setting encodings to null as per the original code logic but is technically supportable
    metadata.setEncodings(null);

    // Assign the rxSource if it does not exist due to the schema requiring it
    if (metadata.getReceivedMessageDetails() != null && metadata.getReceivedMessageDetails().getRxSource() == null) {
      metadata.getReceivedMessageDetails().setRxSource(RxSource.NA);
    }

    if (metadata.getSchemaVersion() <= 4) {
      metadata.setReceivedMessageDetails(null);
    }

    JsonNode messageFrameNode = rootNode.get("payload").get("data").get("MessageFrame");
    MessageFrame<?> messageFrame = simpleXmlMapper.convertValue(messageFrameNode, MessageFrame.class);
    OdeMessageFramePayload payload = new OdeMessageFramePayload(messageFrame);
    return new OdeMessageFrameData(metadata, payload);
  }

  /**
   * Converts the raw IEEE 1609.2 timing values emitted by ASN1C into BSON-Date-compatible values.
   */
  private static SignedDataMetadata extractCertMetadata(JsonNode metadataNode) {
    if (!(metadataNode instanceof ObjectNode metadata)) {
      return null;
    }

    JsonNode header = metadata.remove("signedDataHeaderInfo");
    JsonNode validityPeriod = metadata.remove("signatureValidityPeriod");
    if (header == null && validityPeriod == null) {
      return null;
    }

    SignedDataMetadata result = new SignedDataMetadata();
    if (header != null) {
      result.setPsid(optionalLong(header, "psid"));
      result.setGenerationTime(time64ToDate(optionalLong(header, "generationTime")));
      result.setExpiryTime(time64ToDate(optionalLong(header, "expiryTime")));
    }

    if (validityPeriod != null) {
      Instant certificateStart = time32ToInstant(optionalLong(validityPeriod, "start"));
      if (certificateStart != null) {
        result.setCertificateValidityStart(Date.from(certificateStart));
        result.setCertificateValidityEnd(certificateValidityEnd(certificateStart,
            validityPeriod.get("duration")));
      }
    }

    return result;
  }

  private static Long optionalLong(JsonNode node, String fieldName) {
    JsonNode field = node.get(fieldName);
    return field == null || field.isNull() ? null : field.asLong();
  }

  private static Date time64ToDate(Long time64Microseconds) {
    if (time64Microseconds == null) {
      return null;
    }
    return Date.from(IEEE_1609_2_EPOCH.plusMillis(time64Microseconds / 1_000));
  }

  private static Instant time32ToInstant(Long time32Seconds) {
    return time32Seconds == null ? null : IEEE_1609_2_EPOCH.plusSeconds(time32Seconds);
  }

  private static Date certificateValidityEnd(Instant start, JsonNode duration) {
    if (duration == null || !duration.isObject()) {
      return null;
    }
    if (duration.has("microseconds")) {
      return Date.from(start.plusMillis(duration.get("microseconds").asLong() / 1_000));
    }
    if (duration.has("milliseconds")) {
      return Date.from(start.plusMillis(duration.get("milliseconds").asLong()));
    }
    if (duration.has("seconds")) {
      return Date.from(start.plusSeconds(duration.get("seconds").asLong()));
    }
    if (duration.has("minutes")) {
      return Date.from(start.plusSeconds(duration.get("minutes").asLong() * 60));
    }
    if (duration.has("hours")) {
      return Date.from(start.plusSeconds(duration.get("hours").asLong() * 3_600));
    }
    if (duration.has("sixtyHours")) {
      return Date.from(start.plusSeconds(duration.get("sixtyHours").asLong() * 216_000));
    }
    if (duration.has("years")) {
      return Date.from(start.plusSeconds(duration.get("years").asLong() * 31_536_000));
    }
    return null;
  }
}
