package us.dot.its.jpo.ode.udp.generic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.DatagramPacket;
import org.apache.tomcat.util.buf.HexUtils;
import org.junit.jupiter.api.Test;
import us.dot.its.jpo.ode.uper.UperUtil;

class GenericReceiverPacketTest {

  @Test
  void receivedPayloadHexUsesOffsetAndLengthNotUnusedBufferContents() {
    byte[] received = HexUtils.fromHexString("001400");
    byte[] buffer = new byte[received.length + 4];
    System.arraycopy(received, 0, buffer, 2, received.length);
    buffer[buffer.length - 2] = 0x00;
    buffer[buffer.length - 1] = 0x1f;

    DatagramPacket packet = new DatagramPacket(buffer, 2, received.length);

    assertEquals("001400", GenericReceiver.toReceivedPayloadHex(packet));
    assertEquals("BSM", UperUtil.determineHexPacketType(
        GenericReceiver.toReceivedPayloadHex(packet)));
  }
}
