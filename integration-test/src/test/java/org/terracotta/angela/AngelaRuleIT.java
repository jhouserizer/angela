/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.terracotta.angela.client.Tsa;
import org.terracotta.angela.client.support.junit.AngelaRule;
import org.terracotta.angela.common.topology.Topology;

import java.time.Duration;
import java.util.concurrent.Callable;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.terracotta.angela.client.config.custom.CustomConfigurationContext.customConfigurationContext;
import static org.terracotta.angela.common.TerracottaConfigTool.configTool;
import static org.terracotta.angela.common.dynamic_cluster.Stripe.stripe;
import static org.terracotta.angela.common.provider.DynamicConfigManager.dynamicCluster;
import static org.terracotta.angela.common.tcconfig.TerracottaServer.server;

public class AngelaRuleIT extends BaseIT {

  @Rule public AngelaRule angelaRule = new AngelaRule(
      angelaOrchestrator,
      customConfigurationContext()
          .configTool(context -> context.configTool(configTool("config-tool", hostname)).distribution(getDistribution()))
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
                                  .failoverPriority("availability"),
                              server("server-2", hostname)
                                  .configRepo("terracotta2/repository")
                                  .logs("terracotta2/logs")
                                  .metaData("terracotta2/metadata")
                                  .failoverPriority("availability")
                          )
                      )
                  )
              )
          ), true, true);

  public AngelaRuleIT(String mode, String hostname, boolean inline, boolean ssh) {
    super(mode, hostname, inline, ssh);
  }

  @After
  @Override
  public void close() throws Exception {
    try {
      angelaRule.close();
    } finally {
      super.close();
    }
  }

  @Test
  public void testNodeStartup() {
    Tsa tsa = angelaRule.tsa();
    waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().size(), is(1));
    waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().get(0).size(), is(2));
    waitFor(() -> tsa.getStarted().size(), is(2));
  }

  private void waitFor(Callable<Integer> condition, Matcher<Integer> matcher) {
    await().atMost(Duration.ofSeconds(60)).pollInterval(Duration.ofSeconds(1)).until(condition, matcher);
  }
}
