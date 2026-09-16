package us.dot.its.jpo.ode.udp.portmapped;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.DatagramPacket;
import java.net.InetAddress;
import org.junit.jupiter.api.Test;

class PortMappedConfigurableReceiverTest {

  @Test
  void configuredSourceAddressIsAppliedToRoutedPacket() throws Exception {
    DatagramPacket packet = new DatagramPacket(new byte[1], 1,
        InetAddress.getLoopbackAddress(), 1);

    PortMappedConfigurableReceiver.setConfiguredSourceAddress(packet, "192.0.2.42");

    assertEquals(InetAddress.getByName("192.0.2.42"), packet.getAddress());
  }
}
