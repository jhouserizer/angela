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
package org.terracotta.angela.client.support.junit;

import org.junit.runner.Description;
import org.terracotta.angela.client.AngelaOrchestrator;
import org.terracotta.angela.client.ClientArray;
import org.terracotta.angela.client.ClusterFactory;
import org.terracotta.angela.client.ClusterMonitor;
import org.terracotta.angela.client.ClusterTool;
import org.terracotta.angela.client.ConfigTool;
import org.terracotta.angela.client.Tms;
import org.terracotta.angela.client.Tsa;
import org.terracotta.angela.client.Voter;
import org.terracotta.angela.client.config.ConfigurationContext;
import org.terracotta.angela.common.cluster.Cluster;
import org.terracotta.angela.common.tcconfig.TerracottaServer;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Supplier;

import static java.util.stream.IntStream.rangeClosed;
import static org.terracotta.angela.common.TerracottaServerState.STARTED_AS_ACTIVE;
import static org.terracotta.angela.common.TerracottaServerState.STARTED_AS_PASSIVE;

/**
 * @author Mathieu Carbou
 */
public class AngelaRule extends ExtendedTestRule implements Closeable {

  private final Supplier<AngelaOrchestrator> angelaOrchestratorSupplier;
  private ConfigurationContext configuration;
  private final boolean autoStart;
  private final boolean autoActivate;

  private ClusterFactory clusterFactory;
  private Supplier<Tsa> tsa;
  private Supplier<Cluster> cluster;
  private Supplier<Tms> tms;
  private Supplier<ClientArray> clientArray;
  private Supplier<ClusterMonitor> clusterMonitor;
  private Supplier<Voter> voter;
  private Supplier<ConfigTool> configTool;
  private Supplier<ClusterTool> clusterTool;

  public AngelaRule(AngelaOrchestrator angelaOrchestrator, ConfigurationContext configuration) {
    this(angelaOrchestrator, configuration, false, false);
  }

  public AngelaRule(AngelaOrchestrator angelaOrchestrator, ConfigurationContext configuration, boolean autoStart, boolean autoActivate) {
    this(() -> angelaOrchestrator, configuration, autoStart, autoActivate);
  }

  public AngelaRule(Supplier<AngelaOrchestrator> angelaOrchestratorSupplier, ConfigurationContext configuration, boolean autoStart, boolean autoActivate) {
    this.angelaOrchestratorSupplier = angelaOrchestratorSupplier;
    this.configuration = configuration;
    this.autoStart = autoStart;
    this.autoActivate = autoActivate;
  }

  public ConfigurationContext configure(ConfigurationContext configuration) {
    ConfigurationContext old = this.configuration;
    this.configuration = configuration;
    return old;
  }

  // =========================================
  // junit rule
  // =========================================

  @Override
  protected void before(Description description) throws Throwable {
    String id = description.getTestClass().getSimpleName();
    if (description.getMethodName() != null) {
      id += "." + description.getMethodName();
    }

    AngelaOrchestrator angelaOrchestrator = angelaOrchestratorSupplier.get();
    this.clusterFactory = angelaOrchestrator.newClusterFactory(id, configuration);

    tsa = memoize(clusterFactory::tsa);
    cluster = memoize(clusterFactory::cluster);
    tms = memoize(clusterFactory::tms);
    clientArray = memoize(clusterFactory::firstClientArray);
    clusterMonitor = memoize(clusterFactory::monitor);
    voter = memoize(clusterFactory::voter);
    configTool = memoize(clusterFactory::configTool);
    clusterTool = memoize(clusterFactory::clusterTool);

    if (autoStart) {
      startNodes();
      if (autoActivate) {
        configTool().attachAll();
        configTool().activate();
      }
    }
  }

  @Override
  protected void after(Description description) throws Throwable {
    close();
  }

  @Override
  public void close() throws IOException {
    if (clusterFactory != null) {
      clusterFactory.close();
      clusterFactory = null;
    }
  }

  // =========================================
  // start/stop nodes
  // =========================================

  public void startNodes() {
    List<List<TerracottaServer>> stripes = configuration.tsa().getTopology().getStripes();
    for (int stripeId = 1; stripeId <= stripes.size(); stripeId++) {
      List<TerracottaServer> stripe = stripes.get(stripeId - 1);
      for (int nodeId = 1; nodeId <= stripe.size(); nodeId++) {
        startNode(stripeId, nodeId);
      }
    }
  }

  public void startNode(int stripeId, int nodeId) {
    startNode(getNode(stripeId, nodeId));
  }

  public void startNode(int stripeId, int nodeId, String... cli) {
    startNode(getNode(stripeId, nodeId), cli);
  }

  public void startNode(TerracottaServer node, String... cli) {
    tsa().start(node, cli);
  }

  public void stopNode(int stripeId, int nodeId) {
    tsa().stop(getNode(stripeId, nodeId));
  }

  // =========================================
  // node query
  // =========================================

  public ClusterFactory getClusterFactory() {
    return clusterFactory;
  }

  public ConfigurationContext getConfiguration() {
    return configuration;
  }

  public int getStripeCount() {
    return configuration.tsa().getTopology().getStripes().size();
  }

  public int getNodeCount(int stripeId) {
    return getStripe(stripeId).size();
  }

  public List<TerracottaServer> getStripe(int stripeId) {
    if (stripeId < 1) {
      throw new IllegalArgumentException("Invalid stripe ID: " + stripeId);
    }
    List<List<TerracottaServer>> stripes = configuration.tsa().getTopology().getStripes();
    if (stripeId > stripes.size()) {
      throw new IllegalArgumentException("Invalid stripe ID: " + stripeId + ". There are " + stripes.size() + " stripe(s).");
    }
    return stripes.get(stripeId - 1);
  }

  public TerracottaServer getNode(int stripeId, int nodeId) {
    if (nodeId < 1) {
      throw new IllegalArgumentException("Invalid node ID: " + nodeId);
    }
    List<TerracottaServer> nodes = getStripe(stripeId);
    if (nodeId > nodes.size()) {
      throw new IllegalArgumentException("Invalid node ID: " + nodeId + ". Stripe ID: " + stripeId + " has " + nodes.size() + " nodes.");
    }
    return nodes.get(nodeId - 1);
  }

  public int getNodePort(int stripeId, int nodeId) {
    return getNode(stripeId, nodeId).getTsaPort();
  }

  public int getNodeGroupPort(int stripeId, int nodeId) {
    return getNode(stripeId, nodeId).getTsaGroupPort();
  }

  public OptionalInt findActive(int stripeId) {
    List<TerracottaServer> nodes = getStripe(stripeId);
    return rangeClosed(1, nodes.size())
        .filter(nodeId -> tsa().getState(nodes.get(nodeId - 1)) == STARTED_AS_ACTIVE)
        .findFirst();
  }

  public int[] findPassives(int stripeId) {
    List<TerracottaServer> nodes = getStripe(stripeId);
    return rangeClosed(1, nodes.size())
        .filter(nodeId -> tsa().getState(nodes.get(nodeId - 1)) == STARTED_AS_PASSIVE)
        .toArray();
  }

  // =========================================
  // delegates
  // =========================================

  public Tsa tsa() {
    return tsa.get();
  }

  public ConfigTool configTool() {
    return configTool.get();
  }

  public ClusterTool clusterTool() {
    return clusterTool.get();
  }

  public Cluster cluster() {
    return cluster.get();
  }

  public Tms tms() {
    return tms.get();
  }

  public ClientArray clientArray() {
    return clientArray.get();
  }

  public ClusterMonitor monitor() {
    return clusterMonitor.get();
  }

  public Voter voter() {
    return voter.get();
  }

  // =========================================
  // utils
  // =========================================

  private static <T> Supplier<T> memoize(Supplier<T> supplier) {
    return new Supplier<T>() {
      T t;

      @Override
      public T get() {
        if (t == null) {
          t = supplier.get();
        }
        return t;
      }
    };
  }
}
