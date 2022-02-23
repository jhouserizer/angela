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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.ignite.Ignite;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.cluster.ClusterGroupEmptyException;
import org.apache.ignite.configuration.CollectionConfiguration;
import org.apache.ignite.events.DiscoveryEvent;
import org.apache.ignite.events.Event;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgniteBiPredicate;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.lang.IgnitePredicate;
import org.apache.ignite.lang.IgniteRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.angela.agent.Agent;
import org.terracotta.angela.agent.client.RemoteClientManager;
import org.terracotta.angela.agent.kit.RemoteKitManager;
import org.terracotta.angela.common.clientconfig.ClientId;
import org.terracotta.angela.common.cluster.Cluster;
import org.terracotta.angela.common.distribution.Distribution;
import org.terracotta.angela.common.topology.InstanceId;
import org.terracotta.angela.common.util.IpUtils;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

/**
 * Executor which is using only one local ignite instance, plus eventually one per client job
 */
public class IgniteLocalExecutor implements Executor {
  private final static Logger logger = LoggerFactory.getLogger(IgniteLocalExecutor.class);

  protected final transient Map<String, AgentID> agents = new ConcurrentHashMap<>();
  protected final transient Map<AgentID, CountDownLatch> shutdowns = new ConcurrentHashMap<>();
  protected final UUID group;
  private final AgentID agentID;
  protected final Ignite ignite;

  public IgniteLocalExecutor(Agent agent) {
    this(agent.getGroupId(), agent.getAgentID(), agent.getIgnite());
  }

  public IgniteLocalExecutor(UUID group, AgentID agentID, Ignite ignite) {
    this.group = group;
    this.agentID = agentID;
    this.ignite = ignite;

    agents.put(agentID.getHostName(), agentID);

    // automatically cleanup the list of known agents when they leave
    ignite.events(clusterGroup()).remoteListen(new IgniteBiPredicate<UUID, Event>() {
      private static final long serialVersionUID = 1L;

      @Override
      public boolean apply(UUID uuid, Event event) {
        if (event instanceof DiscoveryEvent) {
          AgentID left = AgentID.valueOf(((DiscoveryEvent) event).eventNode().attribute("angela.nodeName"));
          logger.info("Agent: {} has left cluster group: {}", left, group);
          agents.values().remove(left);
          // keep a history of recorded shutdowns
          CountDownLatch latch = shutdowns.computeIfAbsent(left, agentID1 -> new CountDownLatch(1));
          latch.countDown();
        }
        return true;
      }
    }, new IgnitePredicate<Event>() {
      private static final long serialVersionUID = 1L;

      @Override
      public boolean apply(Event event) {
        return true;
      }
    }, EventType.EVT_NODE_LEFT);
  }

  public Ignite getIgnite() {
    return ignite;
  }

  @Override
  public void close() {
    getGroup().spawnedAgentIDs()
        .filter(isEqual(getLocalAgentID()).negate())
        .forEach(a -> {
          try {
            shutdown(a);
          } catch (TimeoutException e) {
            logger.warn("Agent: {} did not shutdown in stime", a, e);
          }
        });
  }

  @Override
  public void uploadClientJars(AgentID agentID, InstanceId instanceId, List<Path> locations) {
    Future<Void> remoteDownloadFuture = executeAsync(agentID, () -> downloadFiles(instanceId, new RemoteClientManager(instanceId).getClientClasspathRoot()));
    uploadFiles(instanceId, locations, remoteDownloadFuture);
  }

  @Override
  public void uploadKit(AgentID agentID, InstanceId instanceId, Distribution distribution, String kitInstallationName, Path kitInstallationPath) {
    Future<Void> remoteDownloadFuture = executeAsync(agentID, () -> {
      RemoteKitManager remoteKitManager = new RemoteKitManager(instanceId, distribution, kitInstallationName);
      Path installDir = remoteKitManager.getKitInstallationPath().getParent();
      downloadFiles(instanceId, installDir);
    });
    uploadFiles(instanceId, Collections.singletonList(kitInstallationPath), remoteDownloadFuture);
  }

  @Override
  public void shutdown(AgentID agentID) throws TimeoutException {
    if (getLocalAgentID().equals(agentID)) {
      throw new IllegalArgumentException("Cannot kill myself: " + agentID);
    }

    CountDownLatch done = shutdowns.computeIfAbsent(agentID, a -> {
      AgentGroup group = getGroup();
      Collection<AgentID> spawned = group.spawnedAgentIDs().collect(toSet());

      // already closed ?
      if (!group.contains(agentID)) {
        return new CountDownLatch(0);
      }

      // not remotely closeable ?
      if (!spawned.contains(agentID)) {
        throw new IllegalArgumentException("Cannot kill inline agent: " + agentID);
      }

      logger.info("Requesting shutdown of agent: {}", agentID);

      try {
        executeAsync(agentID, () -> new Thread() {
          {setDaemon(true);}

          @SuppressFBWarnings("DM_EXIT")
          @Override
          public void run() {
            try {
              Thread.sleep(1_000);
            } catch (InterruptedException ignored) {
            }
            System.exit(0);
          }
        }.start());
      } catch (ClusterGroupEmptyException e) {
        logger.debug("Agent: {} has been closed concurrently through another mean", agentID);
        return new CountDownLatch(0);
      }

      return new CountDownLatch(1);
    });

    try {
      if (!done.await(15, TimeUnit.SECONDS)) {
        throw new TimeoutException("Agent: " + agentID + " did not shutown within 15 seconds...");
      }
    } catch (InterruptedException e) {
      throw Exceptions.asRuntime(e);
    }
  }

  @Override
  public String toString() {
    return getLocalAgentID().toString();
  }

  @Override
  public AgentID getLocalAgentID() {
    return agentID;
  }

  @Override
  public synchronized Optional<AgentID> findAgentID(String hostname) {
    // agent found ?
    if (agents.containsKey(hostname)) {
      return Optional.of(agents.get(hostname));
    }

    // requested hostname is local (i.e. 127.0.0.1, localhost, etc) and then we would use the started orchestrator for it
    if (IpUtils.isLocal(hostname)) {
      agents.put(hostname, getLocalAgentID());
      return Optional.of(getLocalAgentID());
    }

    // otherwise, it might be an agent spawn but unknown (this should not happen as agents are started from here)
    final List<AgentID> results = getGroup().getPeers().stream().filter(a -> a.getHostName().equals(hostname)).collect(toList());
    if (results.isEmpty()) {
      return Optional.empty();
    }
    if (results.size() == 1) {
      return Optional.of(results.get(0));
    }
    throw new IllegalStateException("Found more than one agent for hostname: " + hostname + ": " + results);
  }

  @Override
  public synchronized AgentGroup getGroup() {
    AgentGroup agentGroup = new AgentGroup(group, clusterGroup().nodes().stream()
        .collect(toMap(
            clusterNode -> AgentID.valueOf(clusterNode.attribute("angela.nodeName")),
            clusterNode -> clusterNode.attributes().entrySet().stream()
                .filter(e -> e.getValue() instanceof String)
                .sorted(Map.Entry.comparingByKey())
                .collect(toMap(
                    Map.Entry::getKey,
                    e -> String.valueOf(e.getValue()),
                    (s, s2) -> {
                      throw new UnsupportedOperationException();
                    },
                    LinkedHashMap::new)))));

    // remove mappings for agents not there anymore
    agents.values().retainAll(agentGroup.getPeers());

    // check if we need to update local map of remote agents
    agentGroup.remoteAgentIDs()
        .filter(agentID -> !agents.containsValue(agentID))
        .forEach(newRemoteAgent -> {
          final String hostname = newRemoteAgent.getHostName();
          final AgentID known = agents.get(hostname);
          if (known == null) {
            logger.info("Discovered remote agent: {} for hostname: {}", newRemoteAgent, hostname);
            agents.put(hostname, newRemoteAgent);
          } else if (!newRemoteAgent.equals(known)) {
            throw new IllegalStateException("Agent: " + newRemoteAgent + " discovered, but we already know remote agent: " + known + " for hostname: " + hostname);
          }
        });

    return agentGroup;
  }

  @Override
  public Cluster getCluster() {
    return new Cluster(ignite, agentID, null);
  }

  @Override
  public Cluster getCluster(ClientId clientId) {
    return new Cluster(ignite, agentID, clientId);
  }

  @Override
  public Future<Void> executeAsync(AgentID agentID, IgniteRunnable job) {
    logger.debug("Executing job on: {}", agentID);
    return new IgniteFutureAdapter<>(agentID, ignite.compute(clusterGroup(agentID)).runAsync(job));
  }

  @Override
  public <R> Future<R> executeAsync(AgentID agentID, IgniteCallable<R> job) {
    logger.debug("Executing job on: {}", agentID);
    return new IgniteFutureAdapter<>(agentID, ignite.compute(clusterGroup(agentID)).callAsync(job));
  }

  @Override
  public BlockingQueue<FileTransfer> getFileTransferQueue(InstanceId instanceId) {
    return ignite.queue(instanceId + "@file-transfer-queue", 500, new CollectionConfiguration().setGroupName(group.toString()));
  }

  @Override
  public Optional<AgentID> startRemoteAgent(String hostname) {
    // we do not use SSH to spawn remote agents: all remote hostnames would be handled from local ignite
    agents.putIfAbsent(hostname, getLocalAgentID());
    return Optional.empty();
  }

  private ClusterGroup clusterGroup() {
    return ignite.cluster().forAttribute("angela.group", group.toString());
  }

  private ClusterGroup clusterGroup(AgentID agentID) {
    final ClusterGroup clusterGroup = ignite.cluster()
        .forAttribute("angela.group", group.toString())
        .forAttribute("angela.nodeName", agentID.toString());
    if (clusterGroup.nodes().isEmpty()) {
      throw new ClusterGroupEmptyException("No agent found matching: " + agentID + " in group " + group);
    }
    if (clusterGroup.nodes().size() > 1) {
      throw new IllegalStateException("Several agents found matching: " + agentID + " in group " + group);
    }
    return clusterGroup;
  }
}
