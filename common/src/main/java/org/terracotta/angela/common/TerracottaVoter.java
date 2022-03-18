/*
 * Copyright Terracotta, Inc.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;

public class TerracottaVoter implements Serializable {
  private static final long serialVersionUID = 1L;

  private final String id;
  private final String hostName;
  private final List<String> hostPorts = new ArrayList<>();
  private final List<String> serverNames = new ArrayList<>();

  private TerracottaVoter(String id, String hostName, List<String> hostPorts, List<String> serverNames) {
    this.id = id;
    this.hostName = hostName;
    this.hostPorts.addAll(hostPorts);
    this.serverNames.addAll(serverNames);
  }

  public static TerracottaVoter voter(String id, String hostName, String... hostPorts) {
    return new TerracottaVoter(id, hostName, Arrays.asList(hostPorts), emptyList());
  }

  public static TerracottaVoter dcVoter(String id, String hostName, String... serverNames) {
    return new TerracottaVoter(id, hostName, emptyList(), Arrays.asList(serverNames));
  }

  public static TerracottaVoter localVoter(String id, String... hostPorts) {
    return new TerracottaVoter(id, IpUtils.getHostName(), Arrays.asList(hostPorts), emptyList());
  }

  public static TerracottaVoter localDCVoter(String id, String... serverNames) {
    return new TerracottaVoter(id, IpUtils.getHostName(), emptyList(), Arrays.asList(serverNames));
  }

  public String getId() {
    return id;
  }

  public String getHostName() {
    return hostName;
  }

  public List<String> getHostPorts() {
    return hostPorts;
  }

  public List<String> getServerNames() {
    return serverNames;
  }
}
