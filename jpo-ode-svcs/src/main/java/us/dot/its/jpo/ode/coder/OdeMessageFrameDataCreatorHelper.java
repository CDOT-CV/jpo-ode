package us.dot.its.jpo.ode.coder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

  // IEEE 1609.2 Duration uses the average Gregorian year, not a 365-day year.
  private static final long SECONDS_PER_YEAR = 31_556_952L;
  private static final long SECONDS_PER_SIXTY_HOURS = 216_000L;
  private static final long SECONDS_PER_HOUR = 3_600L;
  private static final long SECONDS_PER_MINUTE = 60L;

  // Time32 and Time64 both count from this instant. Leap seconds are not applied.
  private static final Instant IEEE_1609_2_EPOCH = Instant.parse("2004-01-01T00:00:00Z");

  // Schema versions 4 and earlier omit receivedMessageDetails.
  private static final int MAX_SCHEMA_VERSION_WITHOUT_RECEIVED_MESSAGE_DETAILS = 4;

  private OdeMessageFrameDataCreatorHelper() {
    throw new UnsupportedOperationException("Utility class should not be instantiated");
  }

  /**
   * Creates an OdeMessageFrameData object from consumed XML data.
   *
   * <p>IEEE 1609.2 {@code signedDataHeaderInfo} and {@code signatureValidityPeriod} elements are
   * converted to {@link SignedDataMetadata} instants and removed before metadata deserialization.
   *
   * @param consumedData The XML data string to be processed
   * @param simpleXmlMapper The XmlMapper for XML operations
   * @return OdeMessageFrameData object containing the processed data
   * @throws JsonProcessingException if there is an error processing the JSON data
   */
  public static OdeMessageFrameData createOdeMessageFrameData(String consumedData, 
      XmlMapper simpleXmlMapper) throws JsonProcessingException {
    JsonNode rootNode = simpleXmlMapper.readTree(consumedData);

    JsonNode metadataNode = rootNode.get("metadata");
    ServiceRequest request = null;
    if (metadataNode instanceof ObjectNode object) {
      if (object.has("request")) {
        JsonNode requestNode = object.get("request");
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

    if (metadata.getSchemaVersion() <= MAX_SCHEMA_VERSION_WITHOUT_RECEIVED_MESSAGE_DETAILS) {
      metadata.setReceivedMessageDetails(null);
    }

    JsonNode messageFrameNode = rootNode.get("payload").get("data").get("MessageFrame");
    MessageFrame<?> messageFrame = simpleXmlMapper.convertValue(messageFrameNode, MessageFrame.class);
    OdeMessageFramePayload payload = new OdeMessageFramePayload(messageFrame);
    return new OdeMessageFrameData(metadata, payload);
  }

  /**
   * Converts ASN1C's raw IEEE 1609.2 XER times into instants.
   *
   * <p>Time64 values keep microsecond precision. Blank or non-numeric text is left unset instead
   * of becoming the 2004 epoch. The source nodes are removed so metadata deserialization ignores
   * them.
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
      result.setGenerationTime(time64ToInstant(optionalLong(header, "generationTime")));
      result.setExpiryTime(time64ToInstant(optionalLong(header, "expiryTime")));
    }

    if (validityPeriod != null) {
      Instant certificateStart = time32ToInstant(optionalLong(validityPeriod, "start"));
      if (certificateStart != null) {
        result.setCertificateValidityStart(certificateStart);
        result.setCertificateValidityEnd(certificateValidityEnd(certificateStart,
            validityPeriod.get("duration")));
      }
    }

    return result;
  }

  /** Returns the field as a long, or null when it is missing, blank, or not an integer. */
  private static Long optionalLong(JsonNode node, String fieldName) {
    JsonNode field = node.get(fieldName);
    if (field == null || field.isNull() || !field.isValueNode()) {
      return null;
    }
    String text = field.asText().trim();
    if (text.isEmpty()) {
      return null;
    }
    try {
      return Long.parseLong(text);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private static Instant time64ToInstant(Long time64Microseconds) {
    if (time64Microseconds == null) {
      return null;
    }
    return IEEE_1609_2_EPOCH.plus(time64Microseconds, ChronoUnit.MICROS);
  }

  private static Instant time32ToInstant(Long time32Seconds) {
    return time32Seconds == null ? null : IEEE_1609_2_EPOCH.plusSeconds(time32Seconds);
  }

  /**
   * Adds the first {@code Duration} arm that parses as an integer.
   *
   * <p>Empty or non-numeric arms are skipped, so a later arm can still apply. {@code years} uses
   * the mean Gregorian year in {@link #SECONDS_PER_YEAR}, not a calendar year.
   */
  private static Instant certificateValidityEnd(Instant start, JsonNode duration) {
    if (duration == null || !duration.isObject()) {
      return null;
    }
    Long microseconds = optionalLong(duration, "microseconds");
    if (microseconds != null) {
      return start.plus(microseconds, ChronoUnit.MICROS);
    }
    Long milliseconds = optionalLong(duration, "milliseconds");
    if (milliseconds != null) {
      return start.plusMillis(milliseconds);
    }
    Long seconds = optionalLong(duration, "seconds");
    if (seconds != null) {
      return start.plusSeconds(seconds);
    }
    Long minutes = optionalLong(duration, "minutes");
    if (minutes != null) {
      return start.plusSeconds(minutes * SECONDS_PER_MINUTE);
    }
    Long hours = optionalLong(duration, "hours");
    if (hours != null) {
      return start.plusSeconds(hours * SECONDS_PER_HOUR);
    }
    Long sixtyHours = optionalLong(duration, "sixtyHours");
    if (sixtyHours != null) {
      return start.plusSeconds(sixtyHours * SECONDS_PER_SIXTY_HOURS);
    }
    Long years = optionalLong(duration, "years");
    if (years != null) {
      return start.plusSeconds(years * SECONDS_PER_YEAR);
    }
    return null;
  }
}
