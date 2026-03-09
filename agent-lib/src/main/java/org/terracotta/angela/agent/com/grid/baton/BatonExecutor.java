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
import io.baton.NodeId;
import io.baton.SshConfig;
import org.terracotta.angela.agent.Agent;
import org.terracotta.angela.agent.com.AgentGroup;
import org.terracotta.angela.agent.com.AgentID;
import org.terracotta.angela.agent.com.Executor;
import org.terracotta.angela.agent.com.FileTransfer;
import org.terracotta.angela.agent.com.RemoteCallable;
import org.terracotta.angela.agent.com.RemoteRunnable;
import org.terracotta.angela.agent.client.RemoteClientManager;
import org.terracotta.angela.agent.kit.RemoteKitManager;
import org.terracotta.angela.common.AngelaProperties;
import org.terracotta.angela.common.clientconfig.ClientId;
import org.terracotta.angela.common.util.IpUtils;
import org.terracotta.angela.common.cluster.Cluster;
import org.terracotta.angela.common.distribution.Distribution;
import org.terracotta.angela.common.topology.InstanceId;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

/**
 * Baton-backed implementation of Angela's {@link Executor} SPI.
 *
 * <p>Parallel to {@link org.terracotta.angela.agent.com.grid.ignite.IgniteLocalExecutor};
 * dispatches jobs through Baton's {@link Fabric} instead of Ignite compute.
 */
class BatonExecutor implements Executor {

  private final Fabric                                   fabric;
  private final UUID                                     groupId;
  private final AgentID                                  localAgentID;
  private final Map<String, BlockingQueue<FileTransfer>> transferQueues = new ConcurrentHashMap<>();

  BatonExecutor(Fabric fabric, UUID groupId, AgentID localAgentID) {
    this.fabric       = fabric;
    this.groupId      = groupId;
    this.localAgentID = localAgentID;
  }

  @Override
  public AgentID getLocalAgentID() {
    return localAgentID;
  }

  @Override
  public Optional<AgentID> findAgentID(String hostname) {
    if (IpUtils.isLocal(hostname)) {
      return Optional.of(getLocalAgentID());
    }
    // Among all nodes on this host, search for the SSH-deployed baton agent
    // (name starts with "agent-") as it is the remote orchestrating process
    List<NodeId> candidates = fabric.getConnectedNodes().stream()
        .filter(n -> n.getHostname().equals(hostname))
        .collect(Collectors.toList());
    return candidates.stream()
        .filter(n -> n.getName().startsWith("agent-"))
        .findFirst()
        .or(() -> candidates.stream().findFirst())
        .map(BatonExecutor::toAngela);
  }

  @Override
  public Optional<AgentID> startRemoteAgent(String hostname) {
    if (IpUtils.isLocal(hostname)) {
      // orchestrator itself serves as the local agent — no SSH needed
      return Optional.empty();
    }
    try {
      String username = AngelaProperties.SSH_USERNAME.getValue();
      String keyPath  = AngelaProperties.SSH_USERNAME_KEY_PATH.getValue();
      int    sshPort  = Integer.parseInt(AngelaProperties.SSH_PORT.getValue());
      SshConfig ssh = (keyPath != null)
          ? SshConfig.of(username, keyPath, sshPort)
          : SshConfig.of(username, "~/.ssh/id_rsa", sshPort);
      NodeId node = fabric.deployAndConnect(hostname, ssh);
      return Optional.of(toAngela(node));
    } catch (UnsupportedOperationException e) {
      return Optional.empty();
    }
  }

  @Override
  public AgentGroup getGroup() {
    return new BatonAgentGroup(groupId, localAgentID, fabric);
  }

  @Override
  public Cluster getCluster() {
    return new Cluster(new BatonClusterPrimitives(fabric), getLocalAgentID(), null);
  }

  @Override
  public Cluster getCluster(ClientId clientId) {
    return new Cluster(new BatonClusterPrimitives(fabric), getLocalAgentID(), clientId);
  }

  @Override
  public Future<Void> executeAsync(AgentID agentID, RemoteRunnable job) {
    return fabric.executeAsync(toNodeId(agentID), job::run);
  }

  @Override
  public <R> Future<R> executeAsync(AgentID agentID, RemoteCallable<R> job) {
    return fabric.executeAsync(toNodeId(agentID), job::call);
  }

  @Override
  public void uploadClientJars(AgentID agentID, InstanceId instanceId, List<Path> locations) {
    Path destRoot = new RemoteClientManager(instanceId).getClientClasspathRoot();
    NodeId node = toNodeId(agentID);
    for (Path root : locations) {
      if (Files.exists(root)) {
        fabric.upload(node, root, angelaRootRelative(destRoot.resolve(root.getFileName().toString())));
      }
    }
  }

  @Override
  public void uploadKit(AgentID agentID, InstanceId instanceId,
                        Distribution distribution, String kitInstallationName, Path kitInstallationPath) {
    RemoteKitManager remoteKitManager = new RemoteKitManager(instanceId, distribution, kitInstallationName);
    Path destDir = remoteKitManager.getKitInstallationPath().getParent();
    fabric.upload(toNodeId(agentID), kitInstallationPath,
        angelaRootRelative(destDir.resolve(kitInstallationPath.getFileName().toString())));
  }

  /**
   * Converts an absolute path rooted at the orchestrator's {@code Agent.ROOT_DIR} into
   * an {@code angela-root://} URI that the remote agent will expand using its own
   * {@code angela.rootDir}. This avoids sending OS-specific absolute paths (e.g. macOS
   * {@code /Users/...}) to a Linux remote agent.
   */
  private static String angelaRootRelative(Path absolutePath) {
    Path relative = Agent.ROOT_DIR.relativize(absolutePath);
    return "angela-root://" + relative.toString().replace(File.separatorChar, '/');
  }

  @Override
  public BlockingQueue<FileTransfer> getFileTransferQueue(InstanceId instanceId) {
    return transferQueues.computeIfAbsent(instanceId + "@file-transfer-queue",
        k -> new LinkedBlockingQueue<>(500));
  }

  @Override
  public Optional<CompletableFuture<Void>> shutdown(AgentID agentID) {
    fabric.disconnect(toNodeId(agentID));
    return Optional.empty();
  }

  @Override
  public void close() {
    // fabric lifecycle is managed by BatonGridProvider
  }

  static NodeId toNodeId(AgentID id) {
    return new NodeId(id.getName(), id.getHostName(), id.getPort(), id.getPid());
  }

  static AgentID toAngela(NodeId n) {
    return new AgentID(n.getName(), n.getHostname(), n.getPort(), n.getPid());
  }
}
