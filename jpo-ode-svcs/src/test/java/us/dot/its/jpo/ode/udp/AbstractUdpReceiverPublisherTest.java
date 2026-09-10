package us.dot.its.jpo.ode.udp;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.DatagramPacket;
import java.net.InetAddress;
import org.apache.tomcat.util.buf.HexUtils;
import org.junit.jupiter.api.Test;

class AbstractUdpReceiverPublisherTest {

  @Test
  void trimToReceivedBytes_dropsUnusedReceiveBufferZeros() throws Exception {
    byte[] message = HexUtils.fromHexString("0014abcd");
    int bufferSize = 8192;
    byte[] buffer = new byte[bufferSize];
    System.arraycopy(message, 0, buffer, 0, message.length);

    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getLoopbackAddress(), 46800);
    packet.setLength(message.length);

    assertEquals(bufferSize, packet.getData().length);
    assertEquals(bufferSize * 2, HexUtils.toHexString(packet.getData()).length());

    DatagramPacket trimmed = AbstractUdpReceiverPublisher.trimToReceivedBytes(packet);

    assertEquals(message.length, trimmed.getLength());
    assertEquals(message.length, trimmed.getData().length);
    assertArrayEquals(message, trimmed.getData());
    assertEquals("0014abcd", HexUtils.toHexString(trimmed.getData()));
    assertEquals(InetAddress.getLoopbackAddress(), trimmed.getAddress());
    assertEquals(46800, trimmed.getPort());
  }

  @Test
  void trimToReceivedBytes_emptyPacketReturnsEmptyData() {
    DatagramPacket packet = new DatagramPacket(new byte[4096], 4096);
    packet.setLength(0);

    DatagramPacket trimmed = AbstractUdpReceiverPublisher.trimToReceivedBytes(packet);

    assertEquals(0, trimmed.getLength());
    assertEquals(0, trimmed.getData().length);
  }
}
