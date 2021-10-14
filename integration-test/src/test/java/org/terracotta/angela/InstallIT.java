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
package org.terracotta.angela;

import org.junit.Rule;
import org.junit.Test;
import org.terracotta.angela.client.ClusterFactory;
import org.terracotta.angela.client.ClusterMonitor;
import org.terracotta.angela.client.Tsa;
import org.terracotta.angela.client.config.ConfigurationContext;
import org.terracotta.angela.client.config.custom.CustomConfigurationContext;
import org.terracotta.angela.client.support.junit.AngelaOrchestratorRule;
import org.terracotta.angela.common.TerracottaServerState;
import org.terracotta.angela.common.metrics.HardwareMetric;
import org.terracotta.angela.common.tcconfig.ServerSymbolicName;
import org.terracotta.angela.common.tcconfig.TcConfig;
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.angela.common.topology.Topology;

import java.io.File;
import java.net.InetAddress;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.terracotta.angela.TestUtils.TC_CONFIG_A;
import static org.terracotta.angela.TestUtils.TC_CONFIG_AP;
import static org.terracotta.angela.Versions.EHCACHE_VERSION;
import static org.terracotta.angela.common.AngelaProperties.SSH_STRICT_HOST_CHECKING;
import static org.terracotta.angela.common.TerracottaServerState.STARTED_AS_ACTIVE;
import static org.terracotta.angela.common.TerracottaServerState.STARTING;
import static org.terracotta.angela.common.TerracottaServerState.STOPPED;
import static org.terracotta.angela.common.distribution.Distribution.distribution;
import static org.terracotta.angela.common.tcconfig.TcConfig.tcConfig;
import static org.terracotta.angela.common.topology.LicenseType.TERRACOTTA_OS;
import static org.terracotta.angela.common.topology.PackageType.KIT;
import static org.terracotta.angela.common.topology.Version.version;

/**
 * @author Aurelien Broszniowski
 */
@SuppressWarnings("serial")
public class InstallIT {

  @Rule
  public AngelaOrchestratorRule angelaOrchestratorRule = new AngelaOrchestratorRule();

  @Test
  public void testHardwareMetricsLogs() throws Exception {
    // no need tp close the reservation or port allocator: the rule will do it
    final int[] ports = angelaOrchestratorRule.getPortAllocator().reserve(4).stream().toArray();

    final File resultPath = new File("target", UUID.randomUUID().toString());

    ConfigurationContext config = CustomConfigurationContext.customConfigurationContext()
        .tsa(tsa -> {
          final TcConfig tcConfig = tcConfig(version(EHCACHE_VERSION), TC_CONFIG_AP);
          Map<ServerSymbolicName, Integer> tsaPorts = new HashMap<ServerSymbolicName, Integer>() {{
            put(new ServerSymbolicName("Server1"), ports[0]);
            put(new ServerSymbolicName("Server2"), ports[1]);
          }};
          Map<ServerSymbolicName, Integer> groupPorts = new HashMap<ServerSymbolicName, Integer>() {{
            put(new ServerSymbolicName("Server1"), ports[2]);
            put(new ServerSymbolicName("Server2"), ports[3]);
          }};
          tcConfig.updateServerTsaPort(tsaPorts);
          tcConfig.updateServerGroupPort(groupPorts);

          tsa.topology(new Topology(distribution(version(EHCACHE_VERSION), KIT, TERRACOTTA_OS),
              tcConfig));
        })
        .monitoring(monitoring -> monitoring.commands(EnumSet.of(HardwareMetric.DISK)));

    try (ClusterFactory factory = angelaOrchestratorRule.newClusterFactory("InstallTest::testHardwareStatsLogs", config)) {
      Tsa tsa = factory.tsa();

      TerracottaServer server = tsa.getServer(0, 0);
      tsa.create(server);
      ClusterMonitor monitor = factory.monitor().startOnAll();

      Thread.sleep(3000);

      monitor.downloadTo(resultPath);
    }

    assertThat(new File(resultPath, "/localhost/disk-stats.log").exists(), is(true));
  }

  @Test
  public void testSsh() throws Exception {
    // no need tp close the reservation or port allocator: the rule will do it
    final int[] ports = angelaOrchestratorRule.getPortAllocator().reserve(2).stream().toArray();

    TcConfig tcConfig = tcConfig(version(EHCACHE_VERSION), TC_CONFIG_A);
    tcConfig.updateServerHost(0, InetAddress.getLocalHost().getHostName());

    Map<ServerSymbolicName, Integer> tsaPorts = new HashMap<ServerSymbolicName, Integer>() {{
      put(new ServerSymbolicName("Server1"), ports[0]);
    }};
    Map<ServerSymbolicName, Integer> groupPorts = new HashMap<ServerSymbolicName, Integer>() {{
      put(new ServerSymbolicName("Server1"), ports[1]);
    }};
    tcConfig.updateServerTsaPort(tsaPorts);
    tcConfig.updateServerGroupPort(groupPorts);

    ConfigurationContext config = CustomConfigurationContext.customConfigurationContext()
        .tsa(tsa -> tsa.topology(new Topology(distribution(version(EHCACHE_VERSION), KIT, TERRACOTTA_OS),
            tcConfig(version(EHCACHE_VERSION), TC_CONFIG_A))));

    SSH_STRICT_HOST_CHECKING.setProperty("false");
    try (ClusterFactory factory = angelaOrchestratorRule.newClusterFactory("InstallTest::testSsh", config)) {
      Tsa tsa = factory.tsa();
      tsa.startAll();
    } finally {
      SSH_STRICT_HOST_CHECKING.clearProperty();
    }
  }

  @Test
  public void testLocalInstall() throws Exception {
    // no need tp close the reservation or port allocator: the rule will do it
    final int[] ports = angelaOrchestratorRule.getPortAllocator().reserve(2).stream().toArray();

    ConfigurationContext config = CustomConfigurationContext.customConfigurationContext()
        .tsa(tsa -> {
          final TcConfig tcConfig = tcConfig(version(EHCACHE_VERSION), TC_CONFIG_A);
          Map<ServerSymbolicName, Integer> tsaPorts = new HashMap<ServerSymbolicName, Integer>() {{
            put(new ServerSymbolicName("Server1"), ports[0]);
          }};
          Map<ServerSymbolicName, Integer> groupPorts = new HashMap<ServerSymbolicName, Integer>() {{
            put(new ServerSymbolicName("Server1"), ports[1]);
          }};
          tcConfig.updateServerTsaPort(tsaPorts);
          tcConfig.updateServerGroupPort(groupPorts);

          tsa
              .topology(new Topology(distribution(version(EHCACHE_VERSION), KIT, TERRACOTTA_OS), tcConfig));
        });

    try (ClusterFactory factory = angelaOrchestratorRule.newClusterFactory("InstallTest::testLocalInstall", config)) {
      Tsa tsa = factory.tsa();
      tsa.startAll();
    }
  }

  @Test
  public void testTwoTsaCustomConfigsFailWithoutMultiConfig() {
    // no need tp close the reservation or port allocator: the rule will do it
    final int[] ports = angelaOrchestratorRule.getPortAllocator().reserve(6).stream().toArray();

    final TcConfig tcConfig = tcConfig(version(EHCACHE_VERSION), TC_CONFIG_A);
    Map<ServerSymbolicName, Integer> tsaPorts1 = new HashMap<ServerSymbolicName, Integer>() {{
      put(new ServerSymbolicName("Server1"), ports[0]);
    }};
    Map<ServerSymbolicName, Integer> groupPorts1 = new HashMap<ServerSymbolicName, Integer>() {{
      put(new ServerSymbolicName("Server1"), ports[1]);
    }};
    tcConfig.updateServerTsaPort(tsaPorts1);
    tcConfig.updateServerGroupPort(groupPorts1);

    Topology topology1 = new Topology(distribution(version(EHCACHE_VERSION), KIT, TERRACOTTA_OS),
        tcConfig);

    final TcConfig tcConfigAP = tcConfig(version(EHCACHE_VERSION), TC_CONFIG_AP);
    Map<ServerSymbolicName, Integer> tsaPorts2 = new HashMap<ServerSymbolicName, Integer>() {{
      put(new ServerSymbolicName("Server1"), ports[2]);
      put(new ServerSymbolicName("Server2"), ports[3]);
    }};
    Map<ServerSymbolicName, Integer> groupPorts2 = new HashMap<ServerSymbolicName, Integer>() {{
      put(new ServerSymbolicName("Server1"), ports[4]);
      put(new ServerSymbolicName("Server2"), ports[5]);
    }};
    tcConfigAP.updateServerTsaPort(tsaPorts2);
    tcConfigAP.updateServerGroupPort(groupPorts2);

    Topology topology2 = new Topology(distribution(version(EHCACHE_VERSION), KIT, TERRACOTTA_OS),
        tcConfigAP);

    try {
      CustomConfigurationContext.customConfigurationContext()
          .tsa(tsa -> tsa.topology(topology1))
          .tsa(tsa -> tsa.topology(topology2));
      fail("expected IllegalStateException");
    } catch (IllegalStateException ise) {
      // expected
    }
  }

  @Test
  public void testStopStalledServer() throws Exception {
    // no need tp close the reservation or port allocator: the rule will do it
    final int[] ports = angelaOrchestratorRule.getPortAllocator().reserve(4).stream().toArray();

    ConfigurationContext config = CustomConfigurationContext.customConfigurationContext()
        .tsa(tsa -> {
              final TcConfig tcConfig = tcConfig(version(EHCACHE_VERSION), getClass().getResource("/configs/tc-config-ap-consistent.xml"));
              Map<ServerSymbolicName, Integer> tsaPorts = new HashMap<ServerSymbolicName, Integer>() {{
                put(new ServerSymbolicName("Server1"), ports[0]);
                put(new ServerSymbolicName("Server2"), ports[1]);
              }};
              Map<ServerSymbolicName, Integer> groupPorts = new HashMap<ServerSymbolicName, Integer>() {{
                put(new ServerSymbolicName("Server1"), ports[2]);
                put(new ServerSymbolicName("Server2"), ports[3]);
              }};
              tcConfig.updateServerTsaPort(tsaPorts);
              tcConfig.updateServerGroupPort(groupPorts);

              tsa
                  .topology(new Topology(distribution(version(EHCACHE_VERSION), KIT, TERRACOTTA_OS),
                      tcConfig));
            }
        );

    try (ClusterFactory factory = angelaOrchestratorRule.newClusterFactory("InstallTest::testStopStalledServer", config)) {
      Tsa tsa = factory.tsa();

      TerracottaServer server = tsa.getServer(0, 0);
      tsa.create(server);

      assertThat(tsa.getState(server), is(STARTING));

      tsa.stop(server);
      assertThat(tsa.getState(server), is(STOPPED));
    }
  }

  @Test
  public void testStartCreatedServer() throws Exception {
    // no need tp close the reservation or port allocator: the rule will do it
    final int[] ports = angelaOrchestratorRule.getPortAllocator().reserve(2).stream().toArray();

    ConfigurationContext config = CustomConfigurationContext.customConfigurationContext()
        .tsa(tsa -> {
              final TcConfig tcConfig = tcConfig(version(EHCACHE_VERSION), TC_CONFIG_A);
              Map<ServerSymbolicName, Integer> tsaPorts = new HashMap<ServerSymbolicName, Integer>() {{
                put(new ServerSymbolicName("Server1"), ports[0]);
              }};
              Map<ServerSymbolicName, Integer> groupPorts = new HashMap<ServerSymbolicName, Integer>() {{
                put(new ServerSymbolicName("Server1"), ports[1]);
              }};
              tcConfig.updateServerTsaPort(tsaPorts);
              tcConfig.updateServerGroupPort(groupPorts);

              tsa
                  .topology(new Topology(distribution(version(EHCACHE_VERSION), KIT, TERRACOTTA_OS),
                      tcConfig));
            }
        );

    try (ClusterFactory factory = angelaOrchestratorRule.newClusterFactory("InstallTest::testStartCreatedServer", config)) {
      Tsa tsa = factory.tsa();

      TerracottaServer server = tsa.getServer(0, 0);
      tsa.create(server);
      tsa.start(server);
      assertThat(tsa.getState(server), is(STARTED_AS_ACTIVE));
    }
  }

  @Test(expected = RuntimeException.class)
  public void testServerStartUpWithArg() throws Exception {
    // no need tp close the reservation or port allocator: the rule will do it
    final int[] ports = angelaOrchestratorRule.getPortAllocator().reserve(2).stream().toArray();

    ConfigurationContext config = CustomConfigurationContext.customConfigurationContext()
        .tsa(tsa -> {
              final TcConfig tcConfig = tcConfig(version(EHCACHE_VERSION), TC_CONFIG_A);
              Map<ServerSymbolicName, Integer> tsaPorts = new HashMap<ServerSymbolicName, Integer>() {{
                put(new ServerSymbolicName("Server1"), ports[0]);
              }};
              Map<ServerSymbolicName, Integer> groupPorts = new HashMap<ServerSymbolicName, Integer>() {{
                put(new ServerSymbolicName("Server1"), ports[1]);
              }};
              tcConfig.updateServerTsaPort(tsaPorts);
              tcConfig.updateServerGroupPort(groupPorts);

              tsa
                  .topology(new Topology(distribution(version(EHCACHE_VERSION), KIT, TERRACOTTA_OS),
                      tcConfig));
            }
        );

    try (ClusterFactory factory = angelaOrchestratorRule.newClusterFactory("InstallTest::testStartCreatedServer", config)) {
      Tsa tsa = factory.tsa();

      TerracottaServer server = tsa.getServer(0, 0);
      // Server start-up must fail due to unknown argument passed
      tsa.start(server, "--some-unknown-argument");
    }
  }


  @Test
  public void testStopPassive() throws Exception {
    // no need tp close the reservation or port allocator: the rule will do it
    final int[] ports = angelaOrchestratorRule.getPortAllocator().reserve(4).stream().toArray();

    ConfigurationContext config = CustomConfigurationContext.customConfigurationContext()
        .tsa(tsa -> {
              final TcConfig tcConfig = tcConfig(version(EHCACHE_VERSION), TC_CONFIG_AP);
              Map<ServerSymbolicName, Integer> tsaPorts = new HashMap<ServerSymbolicName, Integer>() {{
                put(new ServerSymbolicName("Server1"), ports[0]);
                put(new ServerSymbolicName("Server2"), ports[1]);
              }};
              Map<ServerSymbolicName, Integer> groupPorts = new HashMap<ServerSymbolicName, Integer>() {{
                put(new ServerSymbolicName("Server1"), ports[2]);
                put(new ServerSymbolicName("Server2"), ports[3]);
              }};
              tcConfig.updateServerTsaPort(tsaPorts);
              tcConfig.updateServerGroupPort(groupPorts);

              tsa
                  .topology(new Topology(distribution(version(EHCACHE_VERSION), KIT, TERRACOTTA_OS),
                      tcConfig));
            }
        );

    try (ClusterFactory factory = angelaOrchestratorRule.newClusterFactory("InstallTest::testStopPassive", config)) {
      Tsa tsa = factory.tsa();
      tsa.startAll();

      TerracottaServer passive = tsa.getPassive();
      System.out.println("********** stop passive");
      tsa.stop(passive);

      assertThat(tsa.getState(passive), is(TerracottaServerState.STOPPED));
      assertThat(tsa.getPassive(), is(nullValue()));

      System.out.println("********** restart passive");
      tsa.start(passive);
      assertThat(tsa.getState(passive), is(TerracottaServerState.STARTED_AS_PASSIVE));

      TerracottaServer active = tsa.getActive();
      assertThat(tsa.getState(active), is(TerracottaServerState.STARTED_AS_ACTIVE));
      System.out.println("********** stop active");
      tsa.stop(active);
      assertThat(tsa.getState(active), is(TerracottaServerState.STOPPED));

      System.out.println("********** wait for passive to become active");
      await().atMost(15, SECONDS).until(() -> tsa.getState(passive), is(TerracottaServerState.STARTED_AS_ACTIVE));

      System.out.println("********** done, shutting down");
    }
  }
}
