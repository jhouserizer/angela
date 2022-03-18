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
package org.terracotta.angela.client;

import org.apache.ignite.Ignite;
import org.terracotta.angela.agent.Agent;
import org.terracotta.angela.common.net.DefaultPortAllocator;
import org.terracotta.angela.common.net.PortAllocator;

import java.io.IOException;
import java.util.Collections;

/**
 *
 */
public class ClusterAgent implements AutoCloseable {
  private final Agent localAgent;
  private final int igniteDiscoveryPort;
  private final int igniteComPort;
  private final PortAllocator portAllocator;

  public ClusterAgent(boolean localOnly) {
    this.portAllocator = new DefaultPortAllocator();
    this.localAgent = new Agent();
    if (localOnly) {
      this.igniteDiscoveryPort = 0;
      this.igniteComPort = 0;
      this.localAgent.startLocalCluster();
    } else {
      PortAllocator.PortReservation reservation = portAllocator.reserve(2);
      this.igniteDiscoveryPort = reservation.next();
      this.igniteComPort = reservation.next();
      this.localAgent.startCluster(Collections.singleton("localhost:" + igniteDiscoveryPort), "localhost:" + igniteDiscoveryPort, igniteDiscoveryPort, igniteComPort);
    }
  }

  public int getIgniteDiscoveryPort() {
    return igniteDiscoveryPort;
  }

  public int getIgniteComPort() {
    return igniteComPort;
  }

  public Ignite getIgnite() {
    return localAgent.getIgnite();
  }

  public PortAllocator getPortAllocator() {
    return portAllocator;
  }

  @Override
  public void close() throws IOException {
    try {
      localAgent.close();
    } finally {
      portAllocator.close();
    }
  }
}
