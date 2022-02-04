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
import org.apache.ignite.lang.IgniteCallable;
import org.terracotta.angela.agent.Agent;
import org.terracotta.angela.client.util.IgniteClientHelper;
import org.terracotta.angela.common.TerracottaCommandLineEnvironment;
import org.terracotta.angela.common.ToolExecutionResult;
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.angela.common.topology.InstanceId;

public class Jcmd {

  private final TerracottaCommandLineEnvironment tcEnv;
  private final Ignite ignite;
  private final InstanceId instanceId;
  private final TerracottaServer terracottaServer;
  private final int ignitePort;
  private final Client client;

  Jcmd(Ignite ignite, InstanceId instanceId, TerracottaServer terracottaServer, int ignitePort, TerracottaCommandLineEnvironment tcEnv) {
    this.ignite = ignite;
    this.instanceId = instanceId;
    this.terracottaServer = terracottaServer;
    this.ignitePort = ignitePort;
    this.client = null;
    this.tcEnv = tcEnv;
  }

  Jcmd(Ignite ignite, InstanceId instanceId, Client client, int ignitePort, TerracottaCommandLineEnvironment tcEnv) {
    this.ignite = ignite;
    this.instanceId = instanceId;
    this.ignitePort = ignitePort;
    this.terracottaServer = null;
    this.client = client;
    this.tcEnv = tcEnv;
  }

  /**
   * Execute jcmd on the target JVM. This basically creates and execute a command line looking like the following:
   * ${JAVA_HOME}/bin/jcmd &lt;the JVM's PID&gt; &lt;arguments&gt;
   * @param arguments The arguments to pass to jcmd after the PID.
   * @return A representation of the jcmd execution
   */
  public ToolExecutionResult executeCommand(String... arguments) {
    String hostname;
    IgniteCallable<ToolExecutionResult> callable;
    if (terracottaServer != null) {
      hostname = terracottaServer.getHostname();
      callable = () -> Agent.getInstance().getController().serverJcmd(instanceId, terracottaServer, tcEnv, arguments);
    } else if (client != null) {
      hostname = client.getHostname();
      callable = () -> Agent.getInstance().getController().clientJcmd(client.getPid(), tcEnv, arguments);
    } else {
      throw new AssertionError();
    }

    return IgniteClientHelper.executeRemotely(ignite, hostname, ignitePort, callable);
  }

}
