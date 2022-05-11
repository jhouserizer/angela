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

import org.hamcrest.Matcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.terracotta.angela.client.filesystem.RemoteFolder;
import org.terracotta.angela.common.ToolExecutionResult;
import org.terracotta.angela.common.cluster.Cluster;
import org.terracotta.angela.common.tcconfig.TerracottaServer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.IntStream.rangeClosed;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.angela.common.TerracottaServerState.STARTED_AS_ACTIVE;
import static org.terracotta.angela.common.TerracottaServerState.STARTED_AS_PASSIVE;
import static org.terracotta.angela.common.TerracottaServerState.STARTED_IN_DIAGNOSTIC_MODE;
import static org.terracotta.angela.common.TerracottaServerState.STOPPED;
import static org.terracotta.utilities.test.matchers.Eventually.within;

/**
 * @author Mathieu Carbou
 */
public class AngelaRule extends ExtendedTestRule implements AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(AngelaRule.class);

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

  public void init(Description description) {
    before(description);
  }

  @Override
  protected void before(Description description) {
    String id = createTestId(description);

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

    prepareLogging(description);

    if (autoStart) {
      startNodes();
      if (autoActivate) {
        configTool().attachAll();
        configTool().activate();
      }
    }
  }

  protected void prepareLogging(Description description) {
    // logback-test.xml
    Optional.ofNullable(getClass().getResource("/tc-logback.xml")).ifPresent(url -> {
      tsa.get().getTsaConfigurationContext().getTopology().getServers().forEach(s -> {
        RemoteFolder folder = tsa.get().browse(s, "");
        try {
          folder.upload("logback-test.xml", url);
        } catch (IOException exp) {
          logger.warn("unable to upload logback-test.xml configuration: " + url + " on server: " + s.getServerSymbolicName(), exp);
        }
      });
    });

    // logback-ext-test.xml
    Stream.of(
            Optional.ofNullable(description.getAnnotation(ExtraLogging.class))
                .map(ExtraLogging::value)
                .map(s -> getClass().getResource(s))
                .orElse(null), // first possibility: user-defined file
            getClass().getResource("/logback-ext-test.xml")) // second possibility: logback-ext-test.xml
        .filter(Objects::nonNull)
        .findFirst()
        .ifPresent(url -> {
          tsa.get().getTsaConfigurationContext().getTopology().getServers().forEach(s -> {
            RemoteFolder folder = tsa.get().browse(s, "");
            try {
              folder.upload("logback-ext-test.xml", url);
            } catch (IOException exp) {
              logger.warn("unable to upload logback-ext-test.xml configuration: " + url + " on server: " + s.getServerSymbolicName(), exp);
            }
          });
        });

    tsa.get().getTsaConfigurationContext().getTopology().getServers().forEach(s -> {
      try {
        RemoteFolder folder = tsa.get().browse(s, "");
        Properties props = new Properties();
        props.setProperty("serverWorkingDir", folder.getAbsoluteName());
        props.setProperty("serverId", s.getServerSymbolicName().getSymbolicName());
        props.setProperty("test.displayName", description.getDisplayName());
        props.setProperty("test.className", description.getClassName());
        props.setProperty("test.methodName", description.getMethodName());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        props.store(bos, "logging properties");
        bos.close();
        folder.upload("logbackVars.properties", new ByteArrayInputStream(bos.toByteArray()));
      } catch (IOException exp) {
        logger.warn("unable to upload logbackVars.properties on server: " + s.getServerSymbolicName(), exp);
      }
    });
  }

  protected String createTestId(Description description) {
    String id = description.getTestClass().getSimpleName();
    if (description.getMethodName() != null) {
      id += "_" + description.getMethodName();
    }
    return id;
  }

  @Override
  protected void after(Description description) {
    close();
  }

  @Override
  public void close() {
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
    tsa().spawn(node, cli);
  }

  public void stopNode(int stripeId, int nodeId) {
    tsa().stop(getNode(stripeId, nodeId));
    // waitForStopped is required because Ignite has a little cache
    // and when the server is stopped, the angela server state read from the test jvm
    // can still be an old state for some milliseconds.
    // So this ensures the server has completed and also that this completion is seen on the test jvm
    waitForStopped(stripeId, nodeId);
  }

  public final void startNode(TerracottaServer node, Map<String, String> env, String... cli) {
    tsa().spawn(node, env, cli);
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

  public final InetSocketAddress getNodeAddress(int stripeId, int nodeId) {
    return InetSocketAddress.createUnresolved(getNode(stripeId, nodeId).getHostName(), getNodePort(stripeId, nodeId));
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

  public final Path getServerHome() {
    return getServerHome(getNode(1, 1));
  }

  public final Path getServerHome(TerracottaServer server) {
    return Paths.get(tsa().browse(server, "").getAbsoluteName());
  }

  // =========================================
  // assertions
  // =========================================

  public final void waitUntil(ToolExecutionResult result, Matcher<ToolExecutionResult> matcher) {
    waitUntil(() -> result, matcher);
  }

  public final void waitUntilServerStdOut(TerracottaServer server, String matcher) {
    assertThat(() -> serverStdOut(server), within(Duration.ofDays(1)).matches(hasItem(containsString(matcher))));
  }

  public final void assertThatServerStdOut(TerracottaServer server, String matcher) {
    assertThat(serverStdOut(server), hasItem(containsString(matcher)));
  }

  public final void assertThatServerStdOut(TerracottaServer server, Matcher<String> matcher) {
    assertThat(serverStdOut(server), hasItem(matcher));
  }

  public List<String> serverStdOut(int stripeId, int nodeId) {
    return serverStdOut(getNode(stripeId, nodeId));
  }

  public List<String> serverStdOut(TerracottaServer server) {
    try {
      return Files.readAllLines(getServerHome(server).resolve("stdout.txt"));
    } catch (IOException io) {
      return Collections.emptyList();
    }
  }

  public final void waitUntilServerLogs(TerracottaServer server, String matcher) {
    assertThat(() -> serverLogs(server), within(Duration.ofDays(1)).matches(hasItem(containsString(matcher))));
  }

  public final void assertThatServerLogs(TerracottaServer server, String matcher) {
    assertThat(serverLogs(server), hasItem(containsString(matcher)));
  }

  public final void assertThatServerLogs(TerracottaServer server, Matcher<String> matcher) {
    assertThat(serverLogs(server), hasItem(matcher));
  }

  public List<String> serverLogs(int stripeId, int nodeId) {
    return serverLogs(getNode(stripeId, nodeId));
  }

  public List<String> serverLogs(TerracottaServer server) {
    try {
      return Files.readAllLines(getServerHome(server)
          .resolve(server.getLogs())
          .resolve(server.getServerSymbolicName().getSymbolicName())
          .resolve("terracotta.server.log"));
    } catch (IOException io) {
      return Collections.emptyList();
    }
  }

  public final <T> void waitUntil(Supplier<T> callable, Matcher<T> matcher) {
    assertThat(callable, within(Duration.ofDays(1)).matches(matcher));
  }

  public final int waitForActive(int stripeId) {
    waitUntil(() -> findActive(stripeId).isPresent(), is(true));
    return findActive(stripeId).getAsInt();
  }

  public final void waitForActive(int stripeId, int nodeId) {
    waitUntil(() -> tsa().getState(getNode(stripeId, nodeId)), is(equalTo(STARTED_AS_ACTIVE)));
  }

  public final void waitForPassive(int stripeId, int nodeId) {
    waitUntil(() -> tsa().getState(getNode(stripeId, nodeId)), is(equalTo(STARTED_AS_PASSIVE)));
  }

  public final void waitForDiagnostic(int stripeId, int nodeId) {
    waitUntil(() -> tsa().getState(getNode(stripeId, nodeId)), is(equalTo(STARTED_IN_DIAGNOSTIC_MODE)));
  }

  public final void waitForStopped(int stripeId, int nodeId) {
    waitUntil(() -> tsa().getState(getNode(stripeId, nodeId)), is(equalTo(STOPPED)));
  }

  public final int[] waitForPassives(int stripeId) {
    int expectedPassiveCount = getNodeCount(stripeId) - 1;
    waitUntil(() -> findPassives(stripeId).length, is(equalTo(expectedPassiveCount)));
    return findPassives(stripeId);
  }

  public final int[] waitForNPassives(int stripeId, int count) {
    waitUntil(() -> findPassives(stripeId).length, is(greaterThanOrEqualTo(count)));
    return findPassives(stripeId);
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
