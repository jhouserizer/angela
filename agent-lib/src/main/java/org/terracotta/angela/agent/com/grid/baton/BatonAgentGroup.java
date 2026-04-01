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
import org.terracotta.angela.agent.com.AgentGroup;
import org.terracotta.angela.agent.com.AgentID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Baton-backed implementation of Angela's {@link AgentGroup}.
 *
 * <p>The group consists of the local orchestrator agent plus all currently
 * connected remote nodes tracked by the {@link Fabric}.
 */
class BatonAgentGroup extends AgentGroup {

  private static final long serialVersionUID = 1L;

  private final transient Fabric fabric;

  BatonAgentGroup(UUID id, AgentID localAgentId, Fabric fabric) {
    super(id, localAgentId);
    this.fabric = fabric;
  }

  @Override
  public Collection<AgentID> getAllAgents() {
    List<AgentID> agents = new ArrayList<>();
    agents.add(getLocalAgentID()); // orchestrator itself
    if (fabric != null) {
      fabric.getConnectedNodes().forEach(n -> agents.add(BatonExecutor.toAngela(n)));
    }
    return agents;
  }
}
