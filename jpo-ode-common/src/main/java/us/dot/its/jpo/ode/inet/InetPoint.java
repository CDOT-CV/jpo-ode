/*=============================================================================
 * Copyright 2018 572682
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package us.dot.its.jpo.ode.inet;

import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.extern.slf4j.Slf4j;
import us.dot.its.jpo.ode.util.CodecUtils;

/**
 * Represents a network endpoint with an IP address, port, and an optional forward flag.
 * This class provides utilities for handling and representing IP-based network endpoints.
 */
@Slf4j
public class InetPoint {

  public final byte[] address;
  public final int port;
  public final boolean forward;

  public InetPoint(String host, int port, boolean forward) throws UnknownHostException {
    this(InetAddress.getByName(host).getAddress(), port, forward);
  }

  public InetPoint(byte[] address, int port) {
    this(address, port, false);
  }

  /**
   * Constructs an InetPoint object.
   *
   * @param address The byte array representing the IP address of the endpoint. Must not be null.
   * @param port    The port number of the endpoint.
   * @param forward A boolean flag indicating whether the endpoint is configured for forwarding.
   *
   * @throws IllegalArgumentException if the address parameter is null.
   */
  public InetPoint(byte[] address, int port, boolean forward) {
    if (address == null) {
      throw new IllegalArgumentException("IP Address is required");
    }
    this.address = address;
    this.port = port;
    this.forward = forward;
  }

  public InetAddress getInetAddress() throws UnknownHostException {
    return InetAddress.getByAddress(address);
  }

  public boolean isIPv6Address() {
    return address.length == 16;
  }

  @Override
  public String toString() {
    String host = "?";
    try {
      host = InetAddress.getByAddress(address).getHostAddress();
    } catch (UnknownHostException e) {
      log.error("Error", e);
    }
    return String.format("%s { port = %d (0x%x); address = %s (%s, %s); forward = %s }",
        getClass().getSimpleName(),
        port, port,
        CodecUtils.toHex(address), address.length == 4 ? "IPv4" : "IPv6", host,
        forward ? "true" : "false"
    );
  }
}
