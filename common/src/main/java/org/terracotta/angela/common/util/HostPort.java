/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.angela.common.util;

import java.net.InetSocketAddress;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class HostPort {
  private final String hostname;
  private final int port;

  public HostPort(InetSocketAddress address) {
    this(address.getHostString(), address.getPort());
  }

  public HostPort(String hostname, int port) {
    this.hostname = requireNonNull(hostname);
    this.port = port;
  }

  /**
   * @deprecated Use {@link #getHostName()} instead
   */
  @Deprecated
  public String getHostname() {
    return getHostName();
  }

  public String getHostName() {
    return hostname;
  }

  public int getPort() {
    return port;
  }

  public String getHostPort() {
    return encloseInBracketsIfIpv6(hostname) + ":" + port;
  }

  private String encloseInBracketsIfIpv6(String hostname) {
    if (hostname != null && HostAndIpValidator.isValidIPv6(hostname, false)) {
      return "[" + hostname + "]";
    }
    return hostname;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HostPort hostPort = (HostPort) o;
    return port == hostPort.port &&
        hostname.equals(hostPort.hostname);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hostname, port);
  }
}
