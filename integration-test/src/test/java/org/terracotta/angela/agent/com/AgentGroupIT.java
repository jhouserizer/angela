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

import org.junit.After;
import org.junit.Test;
import org.terracotta.angela.agent.Agent;
import org.terracotta.angela.agent.client.RemoteClientManager;
import org.terracotta.angela.common.TerracottaCommandLineEnvironment;
import org.terracotta.angela.common.net.DefaultPortAllocator;
import org.terracotta.angela.common.net.PortAllocator;
import org.terracotta.angela.common.topology.InstanceId;
import org.terracotta.angela.util.SshServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.UUID;

import static java.util.function.Predicate.isEqual;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

/**
 * @author Mathieu Carbou
 */
public class AgentGroupIT {

  PortAllocator portAllocator = new DefaultPortAllocator();

  UUID group = UUID.randomUUID();

  SshServer sshServer = new SshServer(Paths.get("target", "sshd", group.toString()))
      .withPort(portAllocator.reserve(1).next())
      .start();

  Agent agent = Agent.igniteOrchestrator(group, portAllocator);
  AgentID agentID = agent.getAgentID();

  Executor executor = new IgniteSshRemoteExecutor(agent)
      .setStrictHostKeyChecking(false)
      .setPort(sshServer.getPort());

  @After
  public void tearDown() {
    executor.close();
    agent.close();
    sshServer.close();
    portAllocator.close();
  }

  @Test
  public void testGetId() {
    assertEquals(group, executor.getGroup().getId());
  }

  @Test
  public void testGetPeers() throws IOException {
    assertEquals(1, executor.getGroup().size());
    assertTrue(executor.getGroup().contains(agentID));
    AgentID client = spawnClient();
    assertEquals(2, executor.getGroup().size());
    assertTrue(executor.getGroup().contains(client));
  }

  @Test
  public void testRemoteAgentIDs() {
    assumeThat(
        "tests requiring the use of a fake DNS hostname (-Djdk.net.hosts.file=...) cannot run on 1.8",
        System.getProperty("java.version"), not(startsWith("1.8")));

    assertEquals(0, executor.getGroup().remoteAgentIDs().count());

    AgentID remoteAgent = executor.startRemoteAgent("testhostname").get();

    assertEquals(1, executor.getGroup().remoteAgentIDs().count());
    assertEquals(remoteAgent, executor.getGroup().remoteAgentIDs().findFirst().get());
  }

  @Test
  public void testSpawnedAgentIDs() throws IOException {
    assumeThat(
        "tests requiring the use of a fake DNS hostname (-Djdk.net.hosts.file=...) cannot run on 1.8",
        System.getProperty("java.version"), not(startsWith("1.8")));

    assertEquals(0, executor.getGroup().spawnedAgentIDs().count());

    AgentID client = spawnClient();
    AgentID remoteAgent = executor.startRemoteAgent("testhostname").get();

    assertEquals(2, executor.getGroup().spawnedAgentIDs().count());
    assertTrue(executor.getGroup().spawnedAgentIDs().anyMatch(isEqual(client)));
    assertTrue(executor.getGroup().spawnedAgentIDs().anyMatch(isEqual(remoteAgent)));
  }

  @Test
  public void tstGetPeerAddresses() throws IOException {
    AgentID client = spawnClient();
    Collection<String> addresses = executor.getGroup().getPeerAddresses();
    assertEquals(2, addresses.size());
    assertTrue(addresses.contains(client.getAddress().toString()));
    assertTrue(addresses.contains(agentID.getAddress().toString()));
  }

  private AgentID spawnClient() throws IOException {
    final Path jar = Files.list(Paths.get("../agent/target"))
        .filter(Files::isRegularFile)
        .filter(path -> path.getFileName().toString().endsWith("-SNAPSHOT.jar"))
        .filter(path -> path.getFileName().toString().startsWith("angela-agent-"))
        .findFirst().get();

    InstanceId instanceId = new InstanceId(UUID.randomUUID().toString(), "client");
    RemoteClientManager remoteClientManager = new RemoteClientManager(instanceId);
    Path lib = remoteClientManager.getClientClasspathRoot();

    Files.createDirectories(lib);
    org.terracotta.utilities.io.Files.copy(jar, lib.resolve(jar.getFileName()));

    return remoteClientManager.spawnClient(TerracottaCommandLineEnvironment.DEFAULT, executor.getGroup());
  }
}
