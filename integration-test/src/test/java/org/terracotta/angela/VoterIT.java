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

import org.junit.Test;
import org.terracotta.angela.client.ClusterFactory;
import org.terracotta.angela.client.ConfigTool;
import org.terracotta.angela.client.Voter;
import org.terracotta.angela.client.config.ConfigurationContext;
import org.terracotta.angela.common.TerracottaVoter;
import org.terracotta.angela.common.topology.Topology;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.terracotta.angela.client.config.custom.CustomConfigurationContext.customConfigurationContext;
import static org.terracotta.angela.common.TerracottaConfigTool.configTool;
import static org.terracotta.angela.common.TerracottaVoterState.CONNECTED_TO_ACTIVE;
import static org.terracotta.angela.common.dynamic_cluster.Stripe.stripe;
import static org.terracotta.angela.common.provider.DynamicConfigManager.dynamicCluster;
import static org.terracotta.angela.common.tcconfig.TerracottaServer.server;

public class VoterIT extends BaseIT {

  public VoterIT(String mode, String hostname, boolean inline, boolean ssh) {
    super(mode, hostname, inline, ssh);
  }

  @Test
  public void testVoterStartup() throws Exception {
    ConfigurationContext configContext = customConfigurationContext()
        .tsa(tsa -> tsa
            .topology(
                new Topology(
                    getDistribution(),
                    dynamicCluster(
                        stripe(
                            server("server-1", hostname)
                                .configRepo("terracotta1/repository")
                                .logs("terracotta1/logs")
                                .metaData("terracotta1/metadata")
                                .failoverPriority("consistency:1"),
                            server("server-2", hostname)
                                .configRepo("terracotta2/repository")
                                .logs("terracotta2/logs")
                                .metaData("terracotta2/metadata")
                                .failoverPriority("consistency:1"))))))
        .voter(voter -> voter.distribution(getDistribution()).addVoter(TerracottaVoter.dcVoter("voter", hostname, "server-1", "server-2")))
        .configTool(context -> context.distribution(getDistribution()).configTool(configTool("config-tool", hostname)));

    try (ClusterFactory factory = angelaOrchestrator.newClusterFactory("VoterTest::testVoterStartup", configContext)) {
      factory.tsa().startAll();
      ConfigTool configTool = factory.configTool();
      configTool.attachAll();
      configTool.activate();

      Voter voter = factory.voter();
      voter.startAll();
      await()
          .atMost(Duration.ofSeconds(30))
          .until(() -> voter.getTerracottaVoterState(configContext.voter().getTerracottaVoters().get(0)) == CONNECTED_TO_ACTIVE);
    }
  }
}