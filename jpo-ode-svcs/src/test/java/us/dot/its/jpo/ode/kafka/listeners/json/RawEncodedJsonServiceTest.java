package us.dot.its.jpo.ode.kafka.listeners.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import us.dot.its.jpo.ode.model.Asn1Encoding;
import us.dot.its.jpo.ode.model.OdeAsn1Data;
import us.dot.its.jpo.ode.model.OdeHexByteArray;
import us.dot.its.jpo.ode.model.OdeMessageFrameMetadata;
import us.dot.its.jpo.ode.uper.SupportedMessageType;

class RawEncodedJsonServiceTest {

  @Test
  void signedTimRetainsCoerEnvelopeAndDeclaresBothDecoders() throws Exception {
    String signedTim = loadSignedTim();
    String rawTim = """
        {"metadata":{"schemaVersion":9},"payload":{"data":{"bytes":"0001%s"}}}
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
        {"metadata":{"schemaVersion":9,"asn1":"0001%s"},
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

  private String rawMessage(String payloadHex) {
    return """
        {"metadata":{"schemaVersion":9},"payload":{"data":{"bytes":"%s"}}}
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
