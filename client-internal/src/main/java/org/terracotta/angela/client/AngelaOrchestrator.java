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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.angela.agent.Agent;
import org.terracotta.angela.client.config.ConfigurationContext;
import org.terracotta.angela.client.config.ConfigurationContextVisitor;
import org.terracotta.angela.client.config.TsaConfigurationContext;
import org.terracotta.angela.client.config.VoterConfigurationContext;
import org.terracotta.angela.common.TerracottaVoter;
import org.terracotta.angela.common.net.DefaultPortAllocator;
import org.terracotta.angela.common.net.PortAllocator;
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.angela.common.topology.Topology;
import org.terracotta.angela.common.util.IpUtils;

import java.util.Collections;
import java.util.List;

/**
 *
 */
public class AngelaOrchestrator implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(AngelaOrchestrator.class);

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
      this.localAgent = Agent.startCluster(Collections.singleton(IpUtils.getHostName() + ":" + igniteDiscoveryPort), IpUtils.getHostName() + ":" + igniteDiscoveryPort, igniteDiscoveryPort, igniteComPort);
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

    // ensure we allocate some ports to the nodes in the config
    configurationContext.visit(new ConfigurationContextVisitor() {
      @Override
      public void visit(TsaConfigurationContext tsaConfigurationContext) {
        Topology topology = tsaConfigurationContext.getTopology();
        if (topology != null) {
          logger.trace("Allocating ports for servers...");
          topology.init(portAllocator);
        }
      }

      @Override
      public void visit(VoterConfigurationContext voterConfigurationContext) {
        for (TerracottaVoter terracottaVoter : voterConfigurationContext.getTerracottaVoters()) {
          final List<String> hostPorts = terracottaVoter.getHostPorts();
          final List<String> serverNames = terracottaVoter.getServerNames();
          if (hostPorts.isEmpty() && serverNames.isEmpty()) {
            throw new IllegalArgumentException("Voter incorrectly configured: missing hosts/ports or server names");
          }
          if (hostPorts.isEmpty()) {
            // pickups allocated ports
            for (String serverName : serverNames) {
              hostPorts.add(configurationContext.tsa().getTopology().getServers()
                  .stream()
                  .filter(server -> server.getServerSymbolicName().getSymbolicName().equals(serverName))
                  .findFirst()
                  .map(TerracottaServer::getHostPort)
                  .orElseThrow(() -> new IllegalArgumentException("Incorrect voter configuration: server name '" + serverName + "' not found")));
            }
            logger.trace("Voter configured to connect to: " + hostPorts);
          }
        }
      }
    });

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
