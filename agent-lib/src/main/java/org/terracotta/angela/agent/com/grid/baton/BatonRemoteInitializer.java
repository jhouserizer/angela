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

import io.baton.NodeId;
import io.baton.RemoteInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.angela.agent.AgentController;
import org.terracotta.angela.agent.com.AgentID;
import org.terracotta.angela.agent.com.grid.ignite.IgniteSshRemoteExecutor;
import org.terracotta.angela.common.net.DefaultPortAllocator;
import org.terracotta.angela.common.net.PortAllocator;

import java.nio.file.Path;

/**
 * Baton {@link RemoteInitializer} that initializes Angela's {@link AgentController}
 * on the remote agent.
 *
 * <p>Replaces the old reflection-based {@code initAngela()} block in
 * {@code AgentMain}, keeping Angela initialization out of the Baton core and
 * discovered through the standard {@link java.util.ServiceLoader} mechanism.
 */
public class BatonRemoteInitializer implements RemoteInitializer {

  private final static Logger logger = LoggerFactory.getLogger(BatonRemoteInitializer.class);

  @Override
  public void initialize(NodeId agentId, Path workDir) {
    AgentID id = new AgentID(agentId.getName(), agentId.getHostname(),
        agentId.getPort(), agentId.getPid());
    PortAllocator portAllocator = new DefaultPortAllocator();
    AgentController controller = new AgentController(id, portAllocator);
    AgentController.setUniqueInstance(controller);
    logger.info("[baton-agent] Angela AgentController initialized: {}", id);
  }
}
