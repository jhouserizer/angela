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
package org.terracotta.angela.agent.com.grid.baton;

import io.baton.Fabric;
import io.baton.FabricFactory;
import io.baton.NodeId;
import io.baton.core.BatonAgentFabric;
import org.terracotta.angela.agent.com.AgentID;
import org.terracotta.angela.agent.com.Executor;
import org.terracotta.angela.agent.com.grid.ClusterPrimitives;
import org.terracotta.angela.agent.com.grid.GridProvider;
import org.terracotta.angela.common.net.PortAllocator;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

/**
 * Baton-backed implementation of Angela's {@link GridProvider} SPI.
 *
 * <p>When {@code peers} contains a non-empty, non-zero address, this JVM acts as a
 * Baton <em>agent</em> and connects to the existing orchestrator at that address.
 * Otherwise this JVM starts a new Baton orchestrator HTTP server.
 */
public class BatonGridProvider implements GridProvider {

  private final Fabric  fabric;
  private final AgentID agentID;

  public BatonGridProvider(UUID group, String instanceName,
                           PortAllocator portAllocator,
                           Collection<String> peers) {
    // Detect agent mode: peers contains a real (non-zero) orchestrator address
    String orchestratorUrl = peers.stream()
        .filter(p -> !p.isEmpty() && !p.endsWith(":0"))
        .map(p -> "http://" + p)
        .findFirst()
        .orElse(null);

    if (orchestratorUrl != null) {
      // Agent mode: connect to existing orchestrator
      int agentPort;
      try (PortAllocator.PortReservation r = portAllocator.reserve(1)) {
        agentPort = r.next();
      }
      try {
        BatonAgentFabric agentFabric = new BatonAgentFabric(orchestratorUrl, agentPort, instanceName);
        this.fabric  = agentFabric;
        this.agentID = BatonExecutor.toAngela(agentFabric.getLocalNodeId());
      } catch (IOException e) {
        throw new RuntimeException("Failed to connect to Baton orchestrator at " + orchestratorUrl, e);
      }
    } else {
      // Orchestrator mode: start a new Baton HTTP server
      int orchestratorPort;
      try (PortAllocator.PortReservation r = portAllocator.reserve(1)) {
        orchestratorPort = r.next();
      }
      this.fabric = FabricFactory.create(orchestratorPort);
      // Use the routable IP from the orchestrator URL in the AgentID so that
      // getPeerAddresses() returns an address that is reachable from remote machines.
      // fabric.getOrchestratorUrl() is "http://<routeableIP>:<port>" (set by BatonFabric
      // using the first non-loopback IPv4 interface address), whereas
      // local.getHostname() is the local hostname which may not be DNS-resolvable remotely.
      NodeId local = this.fabric.getLocalNodeId();
      int httpPort = this.fabric.getOrchestratorPort();
      String orchestratorHost = local.getHostname();
      String orchUrl = this.fabric.getOrchestratorUrl();
      if (orchUrl != null) {
        // orchUrl is "http://host:port" — extract just the host
        String withoutScheme = orchUrl.substring("http://".length());
        int colonIdx = withoutScheme.lastIndexOf(':');
        if (colonIdx >= 0) {
          orchestratorHost = withoutScheme.substring(0, colonIdx);
        }
      }
      this.agentID = new AgentID(local.getName(), orchestratorHost, httpPort, local.getPid());
    }
  }

  @Override
  public AgentID getAgentID() {
    return agentID;
  }

  @Override
  public Executor createExecutor(UUID group, AgentID agentID) {
    return new BatonExecutor(fabric, group, this.agentID);
  }

  @Override
  public ClusterPrimitives createClusterPrimitives() {
    return new BatonClusterPrimitives(fabric);
  }

  @Override
  public void close() {
    fabric.close();
  }
}
