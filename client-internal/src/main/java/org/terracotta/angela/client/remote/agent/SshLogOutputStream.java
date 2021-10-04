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
package org.terracotta.angela.client.remote.agent;

import net.schmizz.sshj.connection.channel.direct.Session;
import org.terracotta.angela.agent.Agent;
import org.terracotta.angela.common.util.ExternalLoggers;
import org.terracotta.angela.common.util.LogOutputStream;

import java.util.concurrent.CountDownLatch;

/**
 * @author Aurelien Broszniowski
 */

class SshLogOutputStream extends LogOutputStream {

  private final String serverName;
  private final Session.Command cmd;
  private final CountDownLatch started = new CountDownLatch(1);

  SshLogOutputStream(String serverName, Session.Command cmd) {
    this.serverName = serverName;
    this.cmd = cmd;
  }

  @Override
  protected void processLine(String line) {
    ExternalLoggers.sshLogger.info("[{}] {}", serverName, line);
    if (line.contains(Agent.AGENT_IS_READY_MARKER_LOG)) {
      started.countDown();
    }
  }

  public void waitForStartedState() throws InterruptedException {
    if (!cmd.isOpen()) {
      throw new RuntimeException("agent refused to start");
    }
    started.await();
  }

}
