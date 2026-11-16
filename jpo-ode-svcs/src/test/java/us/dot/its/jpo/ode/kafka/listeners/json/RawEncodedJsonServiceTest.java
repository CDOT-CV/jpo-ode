package us.dot.its.jpo.ode.kafka.listeners.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.apache.tomcat.util.buf.HexUtils;
import org.junit.jupiter.api.Test;
import us.dot.its.jpo.ode.model.Asn1Encoding;
import us.dot.its.jpo.ode.model.OdeAsn1Data;
import us.dot.its.jpo.ode.model.OdeHexByteArray;
import us.dot.its.jpo.ode.model.OdeMessageFrameMetadata;
import us.dot.its.jpo.ode.udp.UdpHexDecoder;
import us.dot.its.jpo.ode.uper.SupportedMessageType;

class RawEncodedJsonServiceTest {

  @Test
  void signedTimRetainsCoerEnvelopeAndDeclaresBothDecoders() throws Exception {
    String signedTim = loadSignedTim();
    String rawTim = """
        {"metadata":{"schemaVersion":10},"payload":{"data":{"bytes":"0001%s"}}}
        """.formatted(signedTim);
    RawEncodedJsonService service = new RawEncodedJsonService(new ObjectMapper());

    OdeAsn1Data asn1Data = service.addEncodingAndMutateBytes(
        rawTim, SupportedMessageType.TIM, OdeMessageFrameMetadata.class);

    String decoderBytes = ((OdeHexByteArray) asn1Data.getPayload().getData()).getBytes();
    assertEquals(signedTim, decoderBytes);
    assertEquals("Ieee1609Dot2Data", asn1Data.getMetadata().getEncodings().get(0).getElementType());
    assertEquals("MessageFrame", asn1Data.getMetadata().getEncodings().get(1).getElementType());
  }

  @Test
  void signedTimUsesFullAsn1MetadataWhenPayloadWasPreviouslyStripped() throws Exception {
    String signedTim = loadSignedTim();
    // This fixture contains a 98-byte MessageFrame after its seven-byte COER prefix.
    String strippedTim = signedTim.substring(14, 210);
    String rawTim = """
        {"metadata":{"schemaVersion":10,"asn1":"0001%s"},
        "payload":{"data":{"bytes":"%s"}}}
        """.formatted(signedTim, strippedTim);
    RawEncodedJsonService service = new RawEncodedJsonService(new ObjectMapper());

    OdeAsn1Data asn1Data = service.addEncodingAndMutateBytes(
        rawTim, SupportedMessageType.TIM, OdeMessageFrameMetadata.class);

    String decoderBytes = ((OdeHexByteArray) asn1Data.getPayload().getData()).getBytes();
    assertEquals(signedTim, decoderBytes);
  }

  @Test
  void signedTimDetectionAcceptsUppercaseHex() throws Exception {
    String signedTim = loadSignedTim().toUpperCase(Locale.ROOT);
    String rawJson = rawMessage("0001" + signedTim);

    OdeAsn1Data asn1Data = new RawEncodedJsonService(new ObjectMapper())
        .addEncodingAndMutateBytes(rawJson, SupportedMessageType.TIM,
            OdeMessageFrameMetadata.class);

    assertEquals(signedTim, ((OdeHexByteArray) asn1Data.getPayload().getData()).getBytes());
    assertEquals("Ieee1609Dot2Data", asn1Data.getMetadata().getEncodings().get(0).getElementType());
  }

  @Test
  void markerInsideUnsignedPayloadDoesNotTriggerSignedDetection() throws Exception {
    String unsignedBsm = Files.readString(Path.of(
        "src/test/resources/us/dot/its/jpo/ode/udp/bsm/BsmReceiverTest_ValidBSM.txt"));
    String payloadWithMarker = unsignedBsm.substring(0, 4) + "038100" + unsignedBsm.substring(4);

    OdeAsn1Data asn1Data = new RawEncodedJsonService(new ObjectMapper())
        .addEncodingAndMutateBytes(rawMessage(payloadWithMarker), SupportedMessageType.BSM,
            OdeMessageFrameMetadata.class);

    assertEquals(payloadWithMarker.toUpperCase(Locale.ROOT),
        ((OdeHexByteArray) asn1Data.getPayload().getData()).getBytes());
    assertFalse(asn1Data.getMetadata().getEncodings().stream()
        .map(Asn1Encoding::getElementType).anyMatch("Ieee1609Dot2Data"::equals));
  }

  @Test
  void oddPositionMarkerIsIgnoredWhenValidSignedEnvelopeFollows() throws Exception {
    String signedTim = loadSignedTim();
    String payloadWithOddMarker = "0" + "038100" + "0" + signedTim;

    OdeAsn1Data asn1Data = new RawEncodedJsonService(new ObjectMapper())
        .addEncodingAndMutateBytes(rawMessage(payloadWithOddMarker), SupportedMessageType.TIM,
            OdeMessageFrameMetadata.class);

    assertEquals(signedTim, ((OdeHexByteArray) asn1Data.getPayload().getData()).getBytes());
    assertEquals("Ieee1609Dot2Data", asn1Data.getMetadata().getEncodings().get(0).getElementType());
  }

  @Test
  void signedUdpMessageDeclaresEachEncodingOnce() throws Exception {
    byte[] received = HexUtils.fromHexString("0001" + loadSignedTim());
    DatagramPacket packet = new DatagramPacket(
        received, received.length, InetAddress.getLoopbackAddress(), 1);
    String udpJson = UdpHexDecoder.buildJsonTimFromPacket(packet);

    OdeAsn1Data asn1Data = new RawEncodedJsonService(new ObjectMapper())
        .addEncodingAndMutateBytes(udpJson, SupportedMessageType.TIM,
            OdeMessageFrameMetadata.class);

    assertEquals(List.of("Ieee1609Dot2Data", "MessageFrame"), encodingTypes(asn1Data));
  }

  @Test
  void unsignedUdpMessageDeclaresEachEncodingOnce() throws Exception {
    String unsignedBsm = Files.readString(Path.of(
        "src/test/resources/us/dot/its/jpo/ode/udp/bsm/BsmReceiverTest_ValidBSM.txt")).trim();
    byte[] received = HexUtils.fromHexString(unsignedBsm);
    DatagramPacket packet = new DatagramPacket(
        received, received.length, InetAddress.getLoopbackAddress(), 1);
    String udpJson = UdpHexDecoder.buildJsonBsmFromPacket(packet);

    OdeAsn1Data asn1Data = new RawEncodedJsonService(new ObjectMapper())
        .addEncodingAndMutateBytes(udpJson, SupportedMessageType.BSM,
            OdeMessageFrameMetadata.class);

    assertEquals(List.of("MessageFrame"), encodingTypes(asn1Data));
  }

  @Test
  void encodingsAlreadyInMetadataAreNotDuplicated() throws Exception {
    OdeAsn1Data asn1Data = new RawEncodedJsonService(new ObjectMapper())
        .addEncodingAndMutateBytes(
            rawMessageWithEncoding("MessageFrame", "UPER", "0001" + loadSignedTim()),
            SupportedMessageType.TIM, OdeMessageFrameMetadata.class);

    assertEquals(List.of("Ieee1609Dot2Data", "MessageFrame"), encodingTypes(asn1Data));
  }

  @Test
  void staleSignedEncodingIsDroppedWhenPayloadIsUnsigned() throws Exception {
    String unsignedBsm = Files.readString(Path.of(
        "src/test/resources/us/dot/its/jpo/ode/udp/bsm/BsmReceiverTest_ValidBSM.txt")).trim();

    OdeAsn1Data asn1Data = new RawEncodedJsonService(new ObjectMapper())
        .addEncodingAndMutateBytes(
            rawMessageWithEncoding("Ieee1609Dot2Data", "COER", unsignedBsm),
            SupportedMessageType.BSM, OdeMessageFrameMetadata.class);

    assertEquals(List.of("MessageFrame"), encodingTypes(asn1Data));
  }

  private List<String> encodingTypes(OdeAsn1Data asn1Data) {
    return asn1Data.getMetadata().getEncodings().stream()
        .map(Asn1Encoding::getElementType).toList();
  }

  /** Metadata using the nested {@code encodings} form that {@code EncodingsDeserializer} reads. */
  private String rawMessageWithEncoding(String elementType, String rule, String payloadHex) {
    return """
        {"metadata":{"schemaVersion":10,"encodings":{"encodings":
        {"elementName":"%s","elementType":"%s","encodingRule":"%s"}}},
        "payload":{"data":{"bytes":"%s"}}}
        """.formatted(elementType, elementType, rule, payloadHex);
  }

  private String rawMessage(String payloadHex) {
    return """
        {"metadata":{"schemaVersion":10},"payload":{"data":{"bytes":"%s"}}}
        """.formatted(payloadHex);
  }

  private String loadSignedTim() throws IOException {
    // Existing ACM regression fixture: asn1_codec/data/InputData.decoding.tim.signed.xml.
    try (var input = getClass().getResourceAsStream(
        "/us/dot/its/jpo/ode/services/asn1/signed-tim.hex")) {
      if (input == null) {
        throw new IOException("Signed TIM fixture not found");
      }
      return new String(input.readAllBytes(), StandardCharsets.US_ASCII).trim();
    }
  }
}
