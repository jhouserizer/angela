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
package org.terracotta.angela.common;

import org.terracotta.angela.common.util.IpUtils;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;

public class TerracottaVoter implements Serializable {
  private static final long serialVersionUID = 1L;

  private final String id;
  private final String hostName;
  private final List<InetSocketAddress> hostPorts = new ArrayList<>();
  private final List<String> serverNames = new ArrayList<>();

  private TerracottaVoter(String id, String hostName, List<InetSocketAddress> hostPorts, List<String> serverNames) {
    this.id = id;
    this.hostName = hostName;
    this.hostPorts.addAll(hostPorts);
    this.serverNames.addAll(serverNames);
  }

  public static TerracottaVoter voter(String id, String hostName, InetSocketAddress... hostPorts) {
    return new TerracottaVoter(id, hostName, Arrays.asList(hostPorts), emptyList());
  }

  public static TerracottaVoter voter(String id, String hostName, String... serverNames) {
    return new TerracottaVoter(id, hostName, emptyList(), Arrays.asList(serverNames));
  }

  public static TerracottaVoter localVoter(String id, InetSocketAddress... hostPorts) {
    return new TerracottaVoter(id, IpUtils.getHostName(), Arrays.asList(hostPorts), emptyList());
  }

  public static TerracottaVoter localVoter(String id, String... serverNames) {
    return new TerracottaVoter(id, IpUtils.getHostName(), emptyList(), Arrays.asList(serverNames));
  }

  public String getId() {
    return id;
  }

  public String getHostName() {
    return hostName;
  }

  /**
   * @deprecated backward compatibility: this method was returning the string parameters.
   * Use instead {@link #getHostAddresses()} or {@link #getServerNames()}
   */
  @Deprecated
  public List<String> getHostPorts() {
    return serverNames;
  }

  public List<InetSocketAddress> getHostAddresses() {
    return hostPorts;
  }

  public List<String> getServerNames() {
    return serverNames;
  }
}
