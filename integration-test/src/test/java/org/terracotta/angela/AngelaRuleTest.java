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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.terracotta.angela.client.Tsa;
import org.terracotta.angela.client.support.junit.AngelaOrchestratorRule;
import org.terracotta.angela.client.support.junit.AngelaRule;
import org.terracotta.angela.common.distribution.Distribution;
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

public class AngelaRuleTest {

  private static final Distribution DISTRIBUTION = distribution(version("3.9-SNAPSHOT"), KIT, TERRACOTTA_OS);

  AngelaOrchestratorRule angelaOrchestratorRule = new AngelaOrchestratorRule();
  AngelaRule angelaRule = new AngelaRule(
      () -> angelaOrchestratorRule.getAngelaOrchestrator(),
      customConfigurationContext()
          .configTool(context -> context.configTool(configTool("config-tool", "localhost")).distribution(DISTRIBUTION))
          .tsa(tsa -> tsa
              .topology(
                  new Topology(
                      DISTRIBUTION,
                      dynamicCluster(
                          stripe(
                              server("server-1", "localhost")
                                  .configRepo("terracotta1/repository")
                                  .logs("terracotta1/logs")
                                  .metaData("terracotta1/metadata")
                                  .failoverPriority("availability"),
                              server("server-2", "localhost")
                                  .configRepo("terracotta2/repository")
                                  .logs("terracotta2/logs")
                                  .metaData("terracotta2/metadata")
                                  .failoverPriority("availability")
                          )
                      )
                  )
              )
          ), true, true);

  @Rule public RuleChain ruleChain = RuleChain.emptyRuleChain()
      .around(angelaOrchestratorRule)
      .around(angelaRule);

  @Test
  public void testNodeStartup() throws Exception {
    Tsa tsa = angelaRule.tsa();
    waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().size(), is(1));
    waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().get(0).size(), is(2));
    waitFor(() -> tsa.getStarted().size(), is(2));
  }

  private void waitFor(Callable<Integer> condition, Matcher<Integer> matcher) {
    await().atMost(Duration.ofSeconds(60)).pollInterval(Duration.ofSeconds(1)).until(condition, matcher);
  }
}
