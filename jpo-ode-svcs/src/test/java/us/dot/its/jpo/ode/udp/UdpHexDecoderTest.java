package us.dot.its.jpo.ode.udp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.List;
import org.apache.tomcat.util.buf.HexUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import us.dot.its.jpo.ode.model.OdeAsn1Payload;
import us.dot.its.jpo.ode.model.OdeHexByteArray;
import us.dot.its.jpo.ode.test.utilities.ApprovalTestCase;
import us.dot.its.jpo.ode.uper.SupportedMessageType;
import us.dot.its.jpo.ode.util.CodecUtils;

class UdpHexDecoderTest {

  /**
   * Note: Except for the first 8 and last 4 characters, the rest of the characters
   * in the hexString have been randomly generated.
   */
  String exampleBSMHexString =
      "001480ADDA7CDE5517E962C66947240CB711E804C8B106B7DB7B12B3056B8AA1AA4E838D00400F86822A3CD398D89E1BB8405B72C3C7A398C3CAFF63338526C646F4FFF524AD9E404039D5DA2FA62FEB57E305B552C7BE088B61E52A6BFC8CAF5AF64414F3E4513FEC189F8B5E1138B824A48B29BA1F43CB12CE296BCA3DFA8F651AB44AB1B81B633B797D5645DAA4EDADAB4AC22A0BC38AB361443395BAA2C81CC4538E7413E9C8C3F696BB2C9B6B0000";

  /**
   * Test method for ensuring that the getPayloadHexString method does not result in any missing or extra bytes
   * when retrieving the payload from a BSM message.
   */
  @Test
  void getPayloadHexString_BSM_VerifyNoMissingOrExtraBytes() throws InvalidPayloadException {
    // prepare the received bytes from the example BSM hex string
    byte[] receivedBytes = HexUtils.fromHexString(exampleBSMHexString);

    // prepare the initial buffer and packet to simulate the receive() method
    int bufferSize = 500;
    byte[] buffer = new byte[bufferSize];
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

    // simulate the receive() method by copying the received bytes into the buffer
    System.arraycopy(receivedBytes, 0, buffer, 0, receivedBytes.length);
    packet.setData(buffer);
    packet.setLength(receivedBytes.length);

    // execute the method to get the payload hex string
    OdeAsn1Payload payload = UdpHexDecoder.getPayloadHexString(packet, SupportedMessageType.BSM);

    // verify that the buffer size remains unchanged
    assertEquals(bufferSize, packet.getData().length);

    // verify that the payload contents match the example BSM hex string
    String payloadContents = ((OdeHexByteArray) payload.getData()).getBytes();
    assertEquals(exampleBSMHexString, payloadContents);
  }

  /**
   * metadata.asn1 must use the same uppercase hex as {@link OdeHexByteArray} / {@link CodecUtils}
   * so JSON matches approval fixtures and TIM start flags (e.g. {@code 001f}) still match during
   * stripping (lowercase is used only internally for that path).
   */
  @Test
  void buildJsonMapFromPacket_metadataAsn1MatchesCodecUtils() throws IOException, InvalidPayloadException {
    String path =
        "src/test/resources/us.dot.its.jpo.ode.udp.map/UDPMAP_To_EncodedJSON_Validation.json";
    List<ApprovalTestCase> cases = ApprovalTestCase.deserializeTestCases(path);
    ApprovalTestCase mapCase = cases.stream()
        .filter(c -> c.getDescription().contains("minimum allowed"))
        .findFirst()
        .orElseThrow();

    byte[] receivedBytes = HexUtils.fromHexString(mapCase.getInput());
    DatagramPacket packet = new DatagramPacket(
        receivedBytes, receivedBytes.length, InetAddress.getLoopbackAddress(), 1);

    String json = UdpHexDecoder.buildJsonMapFromPacket(packet);
    String asn1 = new JSONObject(json).getJSONObject("metadata").getString("asn1");

    assertEquals(CodecUtils.toHex(receivedBytes), asn1);
  }
}