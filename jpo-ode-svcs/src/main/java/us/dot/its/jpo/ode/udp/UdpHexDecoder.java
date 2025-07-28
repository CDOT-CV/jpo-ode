package us.dot.its.jpo.ode.udp;

import java.net.DatagramPacket;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.buf.HexUtils;
import us.dot.its.jpo.ode.model.OdeAsn1Data;
import us.dot.its.jpo.ode.model.OdeAsn1Payload;
import us.dot.its.jpo.ode.model.OdeLogMetadata.RecordType;
import us.dot.its.jpo.ode.model.OdeLogMetadata.SecurityResultCode;
import us.dot.its.jpo.ode.model.OdeLogMsgMetadataLocation;
import us.dot.its.jpo.ode.model.OdeMessageFrameMetadata;
import us.dot.its.jpo.ode.model.OdeMessageFrameMetadata.Source;
import us.dot.its.jpo.ode.model.OdeMsgMetadata.GeneratedBy;
import us.dot.its.jpo.ode.model.ReceivedMessageDetails;
import us.dot.its.jpo.ode.model.RxSource;
import us.dot.its.jpo.ode.uper.SupportedMessageType;
import us.dot.its.jpo.ode.uper.UperUtil;
import us.dot.its.jpo.ode.util.DateTimeUtils;
import us.dot.its.jpo.ode.util.JsonUtils;

/**
 * The `UdpHexDecoder` class provides functionalities to decode UDP DatagramPackets into specific
 * JSON representations based on the message type. It supports the extraction and conversion of the
 * packet payloads into ASN.1 payloads for various message types such as MAP, SPAT, TIM, BSM, SSM,
 * SRM, and PSM.
 *
 * <p>The class logs details about the packet's origin and ensures the payload contains the correct
 * message type start flag. It provides methods to build JSON objects encapsulating metadata about
 * each packet's origin, source, record type, and security information.
 *
 * <p>Note that this class cannot be instantiated.
 */
@Slf4j
public class UdpHexDecoder {

  private UdpHexDecoder() {
    throw new UnsupportedOperationException();
  }

  /**
   * Extracts the payload from the given {@link DatagramPacket} and converts it into an
   * {@link OdeAsn1Payload} object. The method validates that the payload contains the necessary
   * start flag for the specified message type.
   *
   * @param packet the DatagramPacket containing the data
   * @param msgType the type of message expected in the payload
   * @return the extracted OdeAsn1Payload from the packet
   * @throws InvalidPayloadException if the payload is null or does not contain the expected start
   *         flag
   */
  public static OdeAsn1Payload getPayloadHexString(DatagramPacket packet,
      SupportedMessageType msgType) throws InvalidPayloadException {
    // retrieve the buffer from the packet
    byte[] buffer = packet.getData();
    if (buffer == null) {
      throw new InvalidPayloadException("Buffer is null, no payload to extract");
    }

    // retrieve the payload from the buffer
    int lengthOfReceivedPacket = packet.getLength();
    int offsetOfReceivedPacket = packet.getOffset();
    byte[] payload = retrieveRelevantBytes(lengthOfReceivedPacket, buffer, offsetOfReceivedPacket);

    // convert bytes to hex string and verify identity
    String payloadHexString = HexUtils.toHexString(payload).toLowerCase();
    if (!payloadHexString.contains(msgType.getStartFlag())) {
      throw new InvalidPayloadException("Payload does not contain start flag");
    }

    log.debug("Full {} packet: {}", msgType, payloadHexString);

    payloadHexString =
        UperUtil.stripDot3Header(payloadHexString, msgType.getStartFlag()).toLowerCase();
    log.debug("Stripped {} packet: {}", msgType, payloadHexString);

    return new OdeAsn1Payload(HexUtils.fromHexString(payloadHexString));
  }

  /**
   * Converts the data from the given {@link DatagramPacket} into a JSON string representing a MAP
   * message. It extracts metadata and payload, then structures them into a JSON format.
   *
   * @param packet the DatagramPacket containing the MAP data
   * @return a JSON string representing the MAP message
   * @throws InvalidPayloadException if the payload extraction fails
   */
  public static String buildJsonMapFromPacket(DatagramPacket packet)
      throws InvalidPayloadException {
    return JsonUtils.toJson(buildAsn1DataFromPacket(packet, SupportedMessageType.MAP,
        RecordType.mapTx, Source.RSU, GeneratedBy.RSU, false), false);
  }

  /**
   * Converts the data from the given {@link DatagramPacket} into a JSON string representing an SPAT
   * message. It extracts metadata and payload, then structures them into a JSON format.
   *
   * @param packet the DatagramPacket containing the SPAT data
   * @return a JSON string representing the SPAT message
   * @throws InvalidPayloadException if the payload extraction fails
   */
  public static String buildJsonSpatFromPacket(DatagramPacket packet)
      throws InvalidPayloadException {
    return JsonUtils.toJson(buildAsn1DataFromPacket(packet, SupportedMessageType.SPAT,
        RecordType.spatTx, Source.RSU, GeneratedBy.RSU, false), false);
  }

  /**
   * Converts the data from the given {@link DatagramPacket} into a JSON string representing a TIM
   * message. It extracts metadata and payload, then structures them into a JSON format.
   *
   * @param packet the DatagramPacket containing the TIM data
   * @return a JSON string representing the TIM message
   * @throws InvalidPayloadException if the payload extraction fails
   */
  public static String buildJsonTimFromPacket(DatagramPacket packet)
      throws InvalidPayloadException {
    return JsonUtils.toJson(buildAsn1DataFromPacket(packet, SupportedMessageType.TIM,
        RecordType.timMsg, Source.RSU, GeneratedBy.RSU, false), false);
  }

  /**
   * Converts the data from the given {@link DatagramPacket} into a JSON string representing a BSM
   * message. It extracts metadata and payload, then structures them into a JSON format.
   *
   * @param packet the DatagramPacket containing the BSM data
   * @return a JSON string representing the BSM message
   * @throws InvalidPayloadException if the payload extraction fails
   */
  public static String buildJsonBsmFromPacket(DatagramPacket packet)
      throws InvalidPayloadException {
    return JsonUtils.toJson(buildAsn1DataFromPacket(packet, SupportedMessageType.BSM,
        RecordType.bsmTx, Source.EV, GeneratedBy.OBU, true), false);
  }

  /**
   * Converts the data from the given {@link DatagramPacket} into a JSON string representing an SSM
   * message. It extracts metadata and payload, then structures them into a JSON format.
   *
   * @param packet the DatagramPacket containing the SSM data
   * @return a JSON string representing the SSM message
   * @throws InvalidPayloadException if the payload extraction fails
   */
  public static String buildJsonSsmFromPacket(DatagramPacket packet)
      throws InvalidPayloadException {
    return JsonUtils.toJson(buildAsn1DataFromPacket(packet, SupportedMessageType.SSM,
      RecordType.ssmTx, Source.RSU, GeneratedBy.RSU, false), false);
  }

  /**
   * Converts the data from the given {@link DatagramPacket} into a JSON string representing an SRM
   * message. It extracts metadata and payload, then structures them into a JSON format.
   *
   * @param packet the DatagramPacket containing the SRM data
   * @return a JSON string representing the SRM message
   * @throws InvalidPayloadException if the payload extraction fails
   */
  public static String buildJsonSrmFromPacket(DatagramPacket packet)
      throws InvalidPayloadException {
    return JsonUtils.toJson(buildAsn1DataFromPacket(packet, SupportedMessageType.SRM,
        RecordType.srmTx, Source.RSU, GeneratedBy.OBU, false), false);
  }

  /**
   * Converts the data from the given {@link DatagramPacket} into a JSON string representing a PSM
   * message. It extracts metadata and payload, then structures them into a JSON format.
   *
   * @param packet the DatagramPacket containing the PSM data
   * @return a JSON string representing the PSM message
   * @throws InvalidPayloadException if the payload extraction fails
   */
  public static String buildJsonPsmFromPacket(DatagramPacket packet)
      throws InvalidPayloadException {
    return JsonUtils.toJson(buildAsn1DataFromPacket(packet, SupportedMessageType.PSM,
        RecordType.psmTx, Source.RSU, GeneratedBy.UNKNOWN, false), false);
  }

  /**
   * Converts the data from the given {@link DatagramPacket} into a JSON string representing a SDSM
   * message. It extracts metadata and payload, then structures them into a JSON format.
   *
   * @param packet the DatagramPacket containing the SDSM data
   * @return a JSON string representing the SDSM message
   * @throws InvalidPayloadException if the payload extraction fails
   */
  public static String buildJsonSdsmFromPacket(DatagramPacket packet)
      throws InvalidPayloadException {
    return JsonUtils.toJson(buildAsn1DataFromPacket(packet, SupportedMessageType.SDSM,
        RecordType.sdsmTx, Source.RSU, GeneratedBy.RSU, false), false);
  }

  /**
   * Converts the data from the given {@link DatagramPacket} into a JSON string representing a RTCM
   * message. It extracts metadata and payload, then structures them into a JSON format.
   *
   * @param packet the DatagramPacket containing the RTCM data
   * @return a JSON string representing the RTCM message
   * @throws InvalidPayloadException if the payload extraction fails
   */
  public static String buildJsonRtcmFromPacket(DatagramPacket packet)
      throws InvalidPayloadException {
    return JsonUtils.toJson(buildAsn1DataFromPacket(packet, SupportedMessageType.RTCM,
        RecordType.rtcmTx, Source.RSU, GeneratedBy.RSU, false), false);
  }

  /**
   * Converts the data from the given {@link DatagramPacket} into a JSON string representing a RSM
   * message. It extracts metadata and payload, then structures them into a JSON format.
   *
   * @param packet the DatagramPacket containing the RSM data
   * @return a JSON string representing the RSM message
   * @throws InvalidPayloadException if the payload extraction fails
   */
  public static String buildJsonRsmFromPacket(DatagramPacket packet)
      throws InvalidPayloadException {
    return JsonUtils.toJson(buildAsn1DataFromPacket(packet, SupportedMessageType.RSM,
        RecordType.rsmTx, Source.RSU, GeneratedBy.RSU, false), false);
  }

  /**
   * Given a buffer containing the full payload, this method retrieves and returns only the relevant
   * bytes of the message, excluding any padded bytes.
   *
   * @param length The length of the message
   * @param buffer The buffer containing the full message and possibly padded bytes
   * @param offset The position in the buffer where the message starts
   * @return The relevant bytes of the message
   */
  private static byte[] retrieveRelevantBytes(int length, byte[] buffer, int offset) {
    byte[] relevantPayload = new byte[length];
    System.arraycopy(buffer, offset, relevantPayload, 0, length);
    return relevantPayload;
  }

  /**
   * Builds an ASN.1 data object from a given DatagramPacket.
   *
   * @param packet The DatagramPacket containing the data
   * @param messageType The type of message to be decoded
   * @param recordType The type of record to be set in the metadata
   * @param source The source of the message
   * @param generatedBy The entity that generated the message
   * @return The ASN.1 data object
   * @throws InvalidPayloadException if the payload extraction fails
   */
  public static OdeAsn1Data buildAsn1DataFromPacket(DatagramPacket packet,
      SupportedMessageType messageType, RecordType recordType, Source source,
      GeneratedBy generatedBy, boolean includeReceivedMessageDetails) throws InvalidPayloadException {

    String senderIp = packet.getAddress().getHostAddress();
    int senderPort = packet.getPort();
    log.debug("Packet received from {}:{}", senderIp, senderPort);

    // Create OdeMsgPayload and OdeLogMetadata objects and populate them
    OdeAsn1Payload payload = getPayloadHexString(packet, messageType);
    OdeMessageFrameMetadata metadata = new OdeMessageFrameMetadata(payload);

    // Add header data for the decoding process
    metadata.setOdeReceivedAt(DateTimeUtils.now());

    metadata.setOriginIp(senderIp);
    metadata.setSource(source);
    metadata.setRecordType(recordType);
    metadata.setRecordGeneratedBy(generatedBy);
    metadata.setSecurityResultCode(SecurityResultCode.success);

    if (includeReceivedMessageDetails) {
      ReceivedMessageDetails receivedMessageDetails = new ReceivedMessageDetails();
      OdeLogMsgMetadataLocation locationData = new OdeLogMsgMetadataLocation("unavailable",
          "unavailable", "unavailable", "unavailable", "unavailable");
      receivedMessageDetails.setRxSource(RxSource.RSU);
      receivedMessageDetails.setLocationData(locationData);
      metadata.setReceivedMessageDetails(receivedMessageDetails);
    }

    return new OdeAsn1Data(metadata, payload);
  }

}
