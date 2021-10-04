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

import org.terracotta.angela.agent.Agent;
import org.terracotta.angela.client.config.ConfigurationContext;
import org.terracotta.angela.common.net.DefaultPortAllocator;
import org.terracotta.angela.common.net.PortAllocator;

import java.util.Collections;

/**
 *
 */
public class AngelaOrchestrator implements AutoCloseable {
  private final Agent localAgent;
  private final PortAllocator portAllocator;

  private AngelaOrchestrator(boolean local) {
    this.portAllocator = new DefaultPortAllocator();
    if (local) {
      this.localAgent = Agent.startLocalCluster();
    } else {
      PortAllocator.PortReservation reservation = portAllocator.reserve(2);
      int igniteDiscoveryPort = reservation.next();
      int igniteComPort = reservation.next();
      this.localAgent = Agent.startCluster(Collections.singleton("localhost:" + igniteDiscoveryPort), "localhost:" + igniteDiscoveryPort, igniteDiscoveryPort, igniteComPort);
    }

    // Since Ignite deported closures require to access a static instance of an agent (Agent.getInstance())
    // started through a main class, even within the same JVM we have to put that instance static to be able
    // to re-use the same closures.
    Agent.setUniqueInstance(localAgent);
  }

  public PortAllocator getPortAllocator() {
    return portAllocator;
  }

  public ClusterFactory newClusterFactory(String idPrefix, ConfigurationContext configurationContext) {
    return new ClusterFactory(localAgent, portAllocator, idPrefix, configurationContext);
  }

  @Override
  public void close() {
    try {
      localAgent.close();
    } finally {
      Agent.removeUniqueInstance(localAgent);
      portAllocator.close();
    }
  }

  public static AgentOrchestratorBuilder builder() {
    return new AgentOrchestratorBuilder();
  }

  public static class AgentOrchestratorBuilder {
    boolean local = false;

    public AgentOrchestratorBuilder local() {
      local = true;
      return this;
    }

    public AngelaOrchestrator build() {
      return new AngelaOrchestrator(local);
    }
  }
}
