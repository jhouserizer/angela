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

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.terracotta.angela.client.ClusterFactory;
import org.terracotta.angela.client.ConfigTool;
import org.terracotta.angela.client.Tsa;
import org.terracotta.angela.client.config.ConfigurationContext;
import org.terracotta.angela.client.config.custom.CustomConfigurationContext;
import org.terracotta.angela.client.support.junit.AngelaOrchestratorRule;
import org.terracotta.angela.common.distribution.Distribution;
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.angela.common.topology.Topology;

import java.time.Duration;
import java.util.concurrent.Callable;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.terracotta.angela.client.config.custom.CustomConfigurationContext.customConfigurationContext;
import static org.terracotta.angela.common.TerracottaConfigTool.configTool;
import static org.terracotta.angela.common.distribution.Distribution.distribution;
import static org.terracotta.angela.common.dynamic_cluster.Stripe.stripe;
import static org.terracotta.angela.common.provider.DynamicConfigManager.dynamicCluster;
import static org.terracotta.angela.common.tcconfig.TerracottaServer.server;
import static org.terracotta.angela.common.topology.LicenseType.TERRACOTTA_OS;
import static org.terracotta.angela.common.topology.PackageType.KIT;
import static org.terracotta.angela.common.topology.Version.version;

public class DynamicClusterTest {
  private static final Duration TIMEOUT = Duration.ofSeconds(60);
  private static final Duration POLL_INTERVAL = Duration.ofSeconds(1);
  private static final Distribution DISTRIBUTION = distribution(version("3.9-SNAPSHOT"), KIT, TERRACOTTA_OS);

  int[] ports;

  @Rule
  public AngelaOrchestratorRule angelaOrchestratorRule = new AngelaOrchestratorRule();

  @Before
  public void setUp() {
    // no need tp close the reservation or port allocator: the rule will do it
    ports = angelaOrchestratorRule.getPortAllocator().reserve(8).stream().toArray();
  }

  @Test
  public void testNodeStartup() throws Exception {
    ConfigurationContext configContext = customConfigurationContext()
        .tsa(tsa -> tsa
            .topology(
                new Topology(
                    DISTRIBUTION,
                    dynamicCluster(
                        stripe(
                            server("server-1", "localhost")
                                .tsaPort(ports[0])
                                .tsaGroupPort(ports[1])
                                .configRepo("terracotta1/repository")
                                .logs("terracotta1/logs")
                                .metaData("terracotta1/metadata")
                                .failoverPriority("availability"),
                            server("server-2", "localhost")
                                .tsaPort(ports[2])
                                .tsaGroupPort(ports[3])
                                .configRepo("terracotta2/repository")
                                .logs("terracotta2/logs")
                                .metaData("terracotta2/metadata")
                                .failoverPriority("availability")
                        )
                    )
                )
            )
        );

    try (ClusterFactory factory = angelaOrchestratorRule.newClusterFactory("DynamicClusterTest::testNodeStartup", configContext)) {
      Tsa tsa = factory.tsa();
      tsa.startAll();

      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().size(), is(1));
      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().get(0).size(), is(2));
      waitFor(() -> tsa.getStarted().size(), is(2));
    }
  }

  @Test
  public void testDynamicNodeAttachToSingleNodeStripe() throws Exception {
    ConfigurationContext configContext = customConfigurationContext()
        .tsa(tsa -> tsa
            .topology(
                new Topology(
                    DISTRIBUTION,
                    dynamicCluster(
                        stripe(
                            server("server-1", "localhost")
                                .tsaPort(ports[0])
                                .tsaGroupPort(ports[1])
                                .configRepo("terracotta1/repository")
                                .logs("terracotta1/logs")
                                .metaData("terracotta1/metadata")
                                .failoverPriority("availability")
                        )
                    )
                )
            )
        ).configTool(context -> context.configTool(configTool("configTool", "localhost")).distribution(DISTRIBUTION));

    try (ClusterFactory factory = angelaOrchestratorRule.newClusterFactory("DynamicClusterTest::testDynamicNodeAttachToSingleNodeStripe", configContext)) {
      Tsa tsa = factory.tsa();
      tsa.startAll();

      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().size(), is(1));
      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().get(0).size(), is(1));

      factory.configTool().attachNode(0, server("server-2", "localhost")
          .tsaPort(ports[2])
          .tsaGroupPort(ports[3])
          .configRepo("terracotta2/repository")
          .logs("terracotta2/logs")
          .metaData("terracotta2/metadata")
          .failoverPriority("availability"));

      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().size(), is(1));
      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().get(0).size(), is(2));
    }
  }

  @Test
  public void testDynamicNodeAttachToMultiNodeStripe() throws Exception {
    ConfigurationContext configContext = customConfigurationContext()
        .tsa(tsa -> tsa
            .topology(
                new Topology(
                    DISTRIBUTION,
                    dynamicCluster(
                        stripe(
                            server("server-1", "localhost")
                                .tsaPort(ports[0])
                                .tsaGroupPort(ports[1])
                                .configRepo("terracotta1/repository")
                                .logs("terracotta1/logs")
                                .metaData("terracotta1/metadata")
                                .failoverPriority("availability"),
                            server("server-2", "localhost")
                                .tsaPort(ports[2])
                                .tsaGroupPort(ports[3])
                                .configRepo("terracotta2/repository")
                                .logs("terracotta2/logs")
                                .metaData("terracotta2/metadata")
                                .failoverPriority("availability")
                        )
                    )
                )
            )
        ).configTool(context -> context.configTool(configTool("configTool", "localhost")).distribution(DISTRIBUTION));

    try (ClusterFactory factory = angelaOrchestratorRule.newClusterFactory("DynamicClusterTest::testDynamicNodeAttachToMultiNodeStripe", configContext)) {
      Tsa tsa = factory.tsa();
      tsa.startAll();

      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().size(), is(1));
      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().get(0).size(), is(2));

      factory.configTool().attachNode(0, server("server-3", "localhost")
          .tsaPort(ports[4])
          .tsaGroupPort(ports[5])
          .configRepo("terracotta3/repository")
          .logs("terracotta3/logs")
          .metaData("terracotta3/metadata")
          .failoverPriority("availability"));

      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().size(), is(1));
      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().get(0).size(), is(3));
    }
  }

  @Test
  public void testDynamicStripeAttachToSingleStripeCluster() throws Exception {
    ConfigurationContext configContext = customConfigurationContext()
        .tsa(tsa -> tsa
            .topology(
                new Topology(
                    DISTRIBUTION,
                    dynamicCluster(
                        stripe(
                            server("server-1", "localhost")
                                .tsaPort(ports[0])
                                .tsaGroupPort(ports[1])
                                .configRepo("terracotta1/repository")
                                .logs("terracotta1/logs")
                                .metaData("terracotta1/metadata")
                                .failoverPriority("availability")
                        )
                    )
                )
            )
        ).configTool(context -> context.configTool(configTool("configTool", "localhost")).distribution(DISTRIBUTION));

    try (ClusterFactory factory = angelaOrchestratorRule.newClusterFactory("DynamicClusterTest::testDynamicStripeAttachToSingleStripeCluster", configContext)) {
      Tsa tsa = factory.tsa();
      tsa.startAll();

      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().size(), is(1));
      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().get(0).size(), is(1));

      factory.configTool().attachStripe(server("server-2", "localhost")
          .tsaPort(ports[2])
          .tsaGroupPort(ports[3])
          .configRepo("terracotta2/repository")
          .logs("terracotta2/logs")
          .metaData("terracotta2/metadata")
          .failoverPriority("availability"));

      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().size(), is(2));
      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().get(0).size(), is(1));
      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().get(1).size(), is(1));
    }
  }

  @Test
  public void testDynamicStripeAttachToMultiStripeCluster() throws Exception {
    ConfigurationContext configContext = customConfigurationContext()
        .tsa(tsa -> tsa
            .topology(
                new Topology(
                    DISTRIBUTION,
                    dynamicCluster(
                        stripe(
                            server("server-1", "localhost")
                                .tsaPort(ports[0])
                                .tsaGroupPort(ports[1])
                                .configRepo("terracotta1/repository")
                                .logs("terracotta1/logs")
                                .metaData("terracotta1/metadata")
                                .failoverPriority("availability")
                        ),
                        stripe(
                            server("server-2", "localhost")
                                .tsaPort(ports[2])
                                .tsaGroupPort(ports[3])
                                .configRepo("terracotta2/repository")
                                .logs("terracotta2/logs")
                                .metaData("terracotta2/metadata")
                                .failoverPriority("availability")
                        )
                    )
                )
            )
        ).configTool(context -> context.configTool(configTool("configTool", "localhost")).distribution(DISTRIBUTION));

    try (ClusterFactory factory = angelaOrchestratorRule.newClusterFactory("DynamicClusterTest::testDynamicStripeAttachToMultiStripeCluster", configContext)) {
      Tsa tsa = factory.tsa();
      tsa.startAll();

      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().size(), is(2));
      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().get(0).size(), is(1));
      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().get(1).size(), is(1));

      factory.configTool().attachStripe(server("server-3", "localhost")
          .tsaPort(ports[4])
          .tsaGroupPort(ports[5])
          .configRepo("terracotta3/repository")
          .logs("terracotta3/logs")
          .metaData("terracotta3/metadata")
          .failoverPriority("availability"));

      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().size(), is(3));
      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().get(0).size(), is(1));
      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().get(1).size(), is(1));
    }
  }

  @Test
  public void testSingleStripeFormation() throws Exception {
    ConfigurationContext configContext = customConfigurationContext()
        .tsa(tsa -> tsa
            .topology(
                new Topology(
                    DISTRIBUTION,
                    dynamicCluster(
                        stripe(
                            server("server-1", "localhost")
                                .tsaPort(ports[0])
                                .tsaGroupPort(ports[1])
                                .configRepo("terracotta1/repository")
                                .logs("terracotta1/logs")
                                .metaData("terracotta1/metadata")
                                .failoverPriority("availability"),
                            server("server-2", "localhost")
                                .tsaPort(ports[2])
                                .tsaGroupPort(ports[3])
                                .configRepo("terracotta2/repository")
                                .logs("terracotta2/logs")
                                .metaData("terracotta2/metadata")
                                .failoverPriority("availability")
                        )
                    )
                )
            )
        ).configTool(context -> context.configTool(configTool("configTool", "localhost")).distribution(DISTRIBUTION));


    try (ClusterFactory factory = angelaOrchestratorRule.newClusterFactory("DynamicClusterTest::testSingleStripeFormation", configContext)) {
      Tsa tsa = factory.tsa();
      tsa.startAll();
      factory.configTool().attachAll();

      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().size(), is(1));
      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().get(0).size(), is(2));
    }
  }

  @Test
  public void testMultiStripeFormation() throws Exception {
    ConfigurationContext configContext = customConfigurationContext()
        .tsa(tsa -> tsa
            .topology(
                new Topology(
                    DISTRIBUTION,
                    dynamicCluster(
                        stripe(
                            server("server-1", "localhost")
                                .tsaPort(ports[0])
                                .tsaGroupPort(ports[1])
                                .configRepo("terracotta1/repository")
                                .logs("terracotta1/logs")
                                .metaData("terracotta1/metadata")
                                .failoverPriority("availability"),
                            server("server-2", "localhost")
                                .tsaPort(ports[2])
                                .tsaGroupPort(ports[3])
                                .configRepo("terracotta2/repository")
                                .logs("terracotta2/logs")
                                .metaData("terracotta2/metadata")
                                .failoverPriority("availability")
                        ),
                        stripe(
                            server("server-3", "localhost")
                                .tsaPort(ports[4])
                                .tsaGroupPort(ports[5])
                                .configRepo("terracotta3/repository")
                                .logs("terracotta3/logs")
                                .metaData("terracotta3/metadata")
                                .failoverPriority("availability"),
                            server("server-4", "localhost")
                                .tsaPort(ports[6])
                                .tsaGroupPort(ports[7])
                                .configRepo("terracotta4/repository")
                                .logs("terracotta4/logs")
                                .metaData("terracotta4/metadata")
                                .failoverPriority("availability")
                        )
                    )
                )
            )
        ).configTool(context -> context.configTool(configTool("configTool", "localhost")).distribution(DISTRIBUTION));

    try (ClusterFactory factory = angelaOrchestratorRule.newClusterFactory("DynamicClusterTest::testMultiStripeFormation", configContext)) {
      Tsa tsa = factory.tsa();
      tsa.startAll();
      factory.configTool().attachAll();

      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().size(), is(2));
      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().get(0).size(), is(2));
      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().get(1).size(), is(2));
    }
  }

  @Test
  public void testDynamicStripeDetach() throws Exception {
    ConfigurationContext configContext = customConfigurationContext()
        .tsa(tsa -> tsa
            .topology(
                new Topology(
                    DISTRIBUTION,
                    dynamicCluster(
                        stripe(
                            server("server-1", "localhost")
                                .tsaPort(ports[0])
                                .tsaGroupPort(ports[1])
                                .configRepo("terracotta1/repository")
                                .logs("terracotta1/logs")
                                .metaData("terracotta1/metadata")
                                .failoverPriority("availability")
                        ),
                        stripe(
                            server("server-2", "localhost")
                                .tsaPort(ports[2])
                                .tsaGroupPort(ports[3])
                                .configRepo("terracotta2/repository")
                                .logs("terracotta2/logs")
                                .metaData("terracotta2/metadata")
                                .failoverPriority("availability")
                        )
                    )
                )
            )
        ).configTool(context -> context.configTool(configTool("configTool", "localhost")).distribution(DISTRIBUTION));

    try (ClusterFactory factory = angelaOrchestratorRule.newClusterFactory("DynamicClusterTest::testDynamicStripeDetach", configContext)) {
      Tsa tsa = factory.tsa();
      tsa.startAll();
      ConfigTool configTool = factory.configTool();
      configTool.attachAll();

      waitFor(() -> tsa.getDiagnosticModeSevers().size(), is(2));
      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().size(), is(2));
      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().get(0).size(), is(1));
      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().get(1).size(), is(1));

      TerracottaServer toDetach = tsa.getServer(1, 0);
      configTool.detachStripe(1);
      tsa.stop(toDetach);

      waitFor(() -> tsa.getDiagnosticModeSevers().size(), is(1));
      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().size(), is(1));
      waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().get(0).size(), is(1));
    }
  }

  @Test
  public void testNodeActivation() throws Exception {
    ConfigurationContext configContext = customConfigurationContext()
        .tsa(tsa -> tsa
            .topology(
                new Topology(
                    DISTRIBUTION,
                    dynamicCluster(
                        stripe(
                            server("server-1", "localhost")
                                .tsaPort(ports[0])
                                .tsaGroupPort(ports[1])
                                .configRepo("terracotta1/repository")
                                .logs("terracotta1/logs")
                                .metaData("terracotta1/metadata")
                                .failoverPriority("availability"),
                            server("server-2", "localhost")
                                .tsaPort(ports[2])
                                .tsaGroupPort(ports[3])
                                .configRepo("terracotta2/repository")
                                .logs("terracotta2/logs")
                                .metaData("terracotta2/metadata")
                                .failoverPriority("availability")
                        )
                    )
                )
            )
        ).configTool(context -> context
            .configTool(configTool("configTool", "localhost"))
            .distribution(DISTRIBUTION));

    try (ClusterFactory factory = angelaOrchestratorRule.newClusterFactory("DynamicClusterTest::testNodeActivation", configContext)) {
      Tsa tsa = factory.tsa();
      tsa.startAll();
      ConfigTool configTool = factory.configTool();
      configTool.attachAll();
      configTool.activate();

      waitFor(() -> tsa.getDiagnosticModeSevers().size(), is(0));
      waitFor(() -> tsa.getActives().size(), is(1));
      waitFor(() -> tsa.getPassives().size(), is(1));
    }
  }

  @Test
  public void testIpv6() throws Exception {
    CustomConfigurationContext configurationContext = customConfigurationContext()
        .tsa(tsa -> tsa
            .topology(
                new Topology(
                    DISTRIBUTION,
                    dynamicCluster(
                        stripe(
                            server("node-1", "localhost")
                                .bindAddress("::")
                                .groupBindAddress("::")
                                .tsaPort(ports[0])
                                .tsaGroupPort(ports[1])
                                .logs("terracotta1/logs")
                                .configRepo("terracotta1/repo")
                                .metaData("terracotta1/metadata")
                                .failoverPriority("availability"),
                            server("node-2", "localhost")
                                .bindAddress("::")
                                .groupBindAddress("::")
                                .tsaPort(ports[2])
                                .tsaGroupPort(ports[3])
                                .logs("terracotta2/logs")
                                .configRepo("terracotta2/repo")
                                .metaData("terracotta2/metadata")
                                .failoverPriority("availability")
                        )
                    )
                )
            )
        ).configTool(context -> context.configTool(configTool("configTool", "localhost")).distribution(DISTRIBUTION));

    try (ClusterFactory factory = angelaOrchestratorRule.newClusterFactory("DynamicClusterTest::testIpv6", configurationContext)) {
      Tsa tsa = factory.tsa();
      tsa.startAll();
      waitFor(() -> tsa.getDiagnosticModeSevers().size(), is(2));

      ConfigTool configTool = factory.configTool();
      configTool.attachAll();
      configTool.activate();
      waitFor(() -> tsa.getActives().size(), is(1));
      waitFor(() -> tsa.getPassives().size(), is(1));
    }
  }

  private void waitFor(Callable<Integer> condition, Matcher<Integer> matcher) {
    await().atMost(TIMEOUT).pollInterval(POLL_INTERVAL).until(condition, matcher);
  }
}
