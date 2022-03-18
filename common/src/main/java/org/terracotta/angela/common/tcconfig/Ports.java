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
package org.terracotta.angela.common.tcconfig;

/**
 * @author Tim Eck
 */
public class Ports {
  private final int tsaPort;
  private final int groupPort;
  private final int managementPort;
  private final int jmxPort;

  public Ports(int tsaPort, int groupPort, int managementPort, int jmxPort) {
    this.tsaPort = tsaPort;
    this.groupPort = groupPort;
    this.managementPort = managementPort;
    this.jmxPort = jmxPort;
  }

  public int getGroupPort() {
    return groupPort;
  }

  public int getManagementPort() {
    return managementPort;
  }

  public int getTsaPort() {
    return tsaPort;
  }

  public int getJmxPort() {
    return jmxPort;
  }

  @Override
  public String toString() {
    return "Ports{" +
           "tsaPort=" + tsaPort +
           ", groupPort=" + groupPort +
           ", managementPort=" + managementPort +
           ", jmxPort=" + jmxPort +
           '}';
  }
}