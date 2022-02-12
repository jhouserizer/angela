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
package org.terracotta.angela.agent.com;

import org.terracotta.angela.agent.Agent;
import org.terracotta.angela.common.util.AngelaVersion;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * @author Mathieu Carbou
 */
public class AgentGroup implements Serializable {
  private static final long serialVersionUID = 1L;

  private final UUID id;
  private final Map<AgentID, Map<String, String>> peers = new LinkedHashMap<>();

  public AgentGroup(UUID id, Map<AgentID, Map<String, String>> peers) {
    this.id = id;
    this.peers.putAll(peers);

    // sanity checks
    for (Map.Entry<AgentID, Map<String, String>> entry : peers.entrySet()) {
      final AgentID agentID = entry.getKey();
      final Map<String, String> attrs = entry.getValue();
      if (!attrs.containsKey("angela.group") || !Objects.equals(attrs.get("angela.group"), id.toString())) {
        throw new IllegalStateException("Agent: " + agentID + " in group: " + attrs.get("angela.group") + " is not part of group: " + id);
      }
      if (!attrs.containsKey("angela.version") || !Objects.equals(attrs.get("angela.version"), AngelaVersion.getAngelaVersion())) {
        throw new IllegalStateException("Agent: " + agentID + " is running version [" + attrs.get("angela.version") + "] but the expected version is [" + AngelaVersion.getAngelaVersion() + "]");
      }
    }
  }

  public UUID getId() {
    return id;
  }

  /**
   * A combination of all Ignite agent launched for both remoting (remote agent) or for running client jobs
   */
  public Collection<AgentID> getPeers() {
    return peers.keySet();
  }

  /**
   * Only the Ignite nodes started remotely, once per hostname.
   */
  public Stream<AgentID> remoteAgentIDs() {
    return getPeers().stream().filter(agentID -> agentID.getName().equals(Agent.AGENT_TYPE_REMOTE));
  }

  /**
   * Only the Ignite nodes spawned
   */
  public Stream<AgentID> spawnedAgentIDs() {
    return peers.entrySet().stream()
        .filter(e -> Objects.equals("spawned", e.getValue().get("angela.process")))
        .map(Map.Entry::getKey);
  }

  public Collection<String> getPeerAddresses() {
    return getPeers().stream().map(AgentID::getAddress).map(Objects::toString).collect(toList());
  }

  @Override
  public String toString() {
    return getId() + "=" + getPeers();
  }

  public int size() {
    return peers.size();
  }

  public boolean isEmpty() {return peers.isEmpty();}

  public boolean contains(AgentID agentID) {return peers.containsKey(agentID);}

  public Stream<AgentID> stream() {return peers.keySet().stream();}

  public void forEach(Consumer<? super AgentID> action) {peers.keySet().forEach(action);}
}
