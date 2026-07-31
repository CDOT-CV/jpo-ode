package us.dot.its.jpo.ode.kafka.listeners.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import us.dot.its.jpo.ode.model.OdeAsn1Data;
import us.dot.its.jpo.ode.model.OdeHexByteArray;
import us.dot.its.jpo.ode.model.OdeMessageFrameMetadata;
import us.dot.its.jpo.ode.uper.SupportedMessageType;

class RawEncodedJsonServiceTest {

  @Test
  void signedTimRetainsCoerEnvelopeAndDeclaresBothDecoders() throws Exception {
    String rawTim = """
        {"metadata":{"schemaVersion":9},"payload":{"data":{"bytes":"000103810000AABB"}}}
        """;
    RawEncodedJsonService service = new RawEncodedJsonService(new ObjectMapper());

    OdeAsn1Data asn1Data = service.addEncodingAndMutateBytes(
        rawTim, SupportedMessageType.TIM, OdeMessageFrameMetadata.class);

    String decoderBytes = ((OdeHexByteArray) asn1Data.getPayload().getData()).getBytes();
    assertEquals("03810000AABB", decoderBytes);
    assertEquals("Ieee1609Dot2Data", asn1Data.getMetadata().getEncodings().get(0).getElementType());
    assertEquals("MessageFrame", asn1Data.getMetadata().getEncodings().get(1).getElementType());
  }

  @Test
  void signedTimUsesFullAsn1MetadataWhenPayloadWasPreviouslyStripped() throws Exception {
    String rawTim = """
        {"metadata":{"schemaVersion":9,"asn1":"000103810000AABB"},
        "payload":{"data":{"bytes":"DEADBEEF"}}}
        """;
    RawEncodedJsonService service = new RawEncodedJsonService(new ObjectMapper());

    OdeAsn1Data asn1Data = service.addEncodingAndMutateBytes(
        rawTim, SupportedMessageType.TIM, OdeMessageFrameMetadata.class);

    String decoderBytes = ((OdeHexByteArray) asn1Data.getPayload().getData()).getBytes();
    assertEquals("03810000AABB", decoderBytes);
  }
}
