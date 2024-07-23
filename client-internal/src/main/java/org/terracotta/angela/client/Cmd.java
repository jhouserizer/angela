/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import org.terracotta.angela.agent.AgentController;
import org.terracotta.angela.agent.com.AgentID;
import org.terracotta.angela.agent.com.Executor;
import org.terracotta.angela.common.ToolExecutionResult;
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.angela.common.topology.InstanceId;

/**
 * Executes one of the scripts in the kit, the path to the command from the base of the kit must be provided
 * e.g. : cmd.executeCommand("server/bin/server-stat");  (The OS extension .bat or .sh is automatically added)
 *
 * @author Aurelien Broszniowski
 */

public class Cmd {

  private final Executor executor;
  private final InstanceId instanceId;
  private final TerracottaServer terracottaServer;

  Cmd(Executor executor, InstanceId instanceId, TerracottaServer terracottaServer) {
    this.executor = executor;
    this.instanceId = instanceId;
    this.terracottaServer = terracottaServer;
  }

  public ToolExecutionResult executeCommand(String terracottaCommand, String... arguments) {
    if (terracottaServer != null) {
      final AgentID agentID = executor.getAgentID(terracottaServer.getHostName());
      return executor.execute(agentID, () -> AgentController.getInstance()
          .serverCmd(instanceId, terracottaServer, terracottaCommand, arguments));
    } else {
      throw new AssertionError();
    }
  }
}