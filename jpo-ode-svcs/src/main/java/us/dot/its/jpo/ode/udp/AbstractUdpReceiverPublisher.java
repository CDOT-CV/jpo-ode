package us.dot.its.jpo.ode.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract base class for UDP receiver publishers that handle incoming UDP messages. Provides
 * common functionality for creating and managing UDP sockets.
 */
@Slf4j
public abstract class AbstractUdpReceiverPublisher implements Runnable {

  /**
   * Exception thrown when there is an error in UDP receiver operations.
   */
  public class UdpReceiverException extends Exception {
    private static final long serialVersionUID = 1L;

    public UdpReceiverException(String string, Exception e) {
      super(string, e);
    }
  }

  protected DatagramSocket socket;

  protected String senderIp;
  protected int senderPort;

  protected int port;
  protected int bufferSize;

  private boolean stopped = false;

  public boolean isStopped() {
    return stopped;
  }

  public void setStopped(boolean stopped) {
    this.stopped = stopped;
  }

  protected AbstractUdpReceiverPublisher(int port, int bufferSize) {
    this.port = port;
    this.bufferSize = bufferSize;

    try {
      this.socket = new DatagramSocket(this.port);
      log.info("Created UDP socket bound to port {}", this.port);
    } catch (SocketException e) {
      log.error("Error creating socket with port {}", this.port, e);
    }
  }

  /**
   * Receives the next datagram and returns a packet whose data array is exactly the received UDP
   * payload.
   *
   * <p>{@link DatagramPacket#getData()} always returns the full receive buffer allocated from
   * {@code ode.receivers.*.buffer-size}. Unused tail bytes are {@code 0x00}. This method copies
   * only {@code offset .. offset + length} so logs and decoders cannot pick up that padding.
   *
   * <p>A new {@link DatagramPacket} is created for each receive so {@code length} is reset to the
   * buffer size. Reusing one packet without {@code setLength(buffer.length)} truncates later,
   * larger datagrams.
   */
  protected DatagramPacket receiveExactPacket() throws IOException {
    byte[] buffer = new byte[bufferSize];
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    socket.receive(packet);
    return trimToReceivedBytes(packet);
  }

  /**
   * Returns a packet backed by a copy of the received bytes only, dropping unused receive-buffer
   * zeros.
   *
   * @param packet the packet filled by {@link DatagramSocket#receive(DatagramPacket)}
   * @return a packet whose {@link DatagramPacket#getData()} length equals the UDP payload length
   */
  public static DatagramPacket trimToReceivedBytes(DatagramPacket packet) {
    if (packet == null || packet.getData() == null || packet.getLength() <= 0) {
      return new DatagramPacket(new byte[0], 0);
    }

    int offset = packet.getOffset();
    int length = packet.getLength();
    byte[] received = Arrays.copyOfRange(packet.getData(), offset, offset + length);

    DatagramPacket trimmed = new DatagramPacket(received, received.length);
    if (packet.getAddress() != null) {
      trimmed.setAddress(packet.getAddress());
      trimmed.setPort(packet.getPort());
    }
    return trimmed;
  }

}
