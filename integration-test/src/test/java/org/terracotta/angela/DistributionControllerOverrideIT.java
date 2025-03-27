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
package org.terracotta.angela;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.terracotta.angela.client.AngelaOrchestrator;
import org.terracotta.angela.client.Tsa;
import org.terracotta.angela.client.support.junit.AngelaRule;
import org.terracotta.angela.common.topology.Topology;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.terracotta.angela.client.config.custom.CustomConfigurationContext.customConfigurationContext;
import static org.terracotta.angela.common.TerracottaConfigTool.configTool;
import org.terracotta.angela.common.distribution.Distribution;
import org.terracotta.angela.common.distribution.Distribution107InlineController;
import org.terracotta.angela.common.net.DefaultPortAllocator;
import org.terracotta.angela.common.net.PortAllocator;
import static org.terracotta.angela.common.dynamic_cluster.Stripe.stripe;
import static org.terracotta.angela.common.provider.DynamicConfigManager.dynamicCluster;
import static org.terracotta.angela.common.tcconfig.TerracottaServer.server;
import org.terracotta.angela.common.topology.LicenseType;
import org.terracotta.angela.common.topology.PackageType;
import org.terracotta.angela.common.topology.Version;
import org.terracotta.angela.common.util.IpUtils;
import org.terracotta.angela.util.Versions;
import net.schmizz.sshj.common.IOUtils;

public class DistributionControllerOverrideIT {

  static Distribution DISTRIBUTION = Distribution.distribution(
    Version.version(Versions.EHCACHE_VERSION_DC), 
    PackageType.KIT, 
    LicenseType.TERRACOTTA_OS)
  // force Distribution107InlineController
  .withDistributionController(Distribution107InlineController.class);

  PortAllocator portAllocator = new DefaultPortAllocator();
  AngelaOrchestrator angelaOrchestrator = AngelaOrchestrator.builder()
    .withPortAllocator(portAllocator)
    .igniteFree()
    .build();

  @Rule
  public Timeout timeout = Timeout.builder().withTimeout(4, TimeUnit.MINUTES).build();

  @Rule public AngelaRule angelaRule = new AngelaRule(
      angelaOrchestrator,
      customConfigurationContext()
          .configTool(context -> context
                  .configTool(configTool("config-tool", IpUtils.getHostName()))
                  .distribution(DISTRIBUTION))
          .tsa(tsa -> tsa
              .topology(
                  new Topology(
                    DISTRIBUTION,
                      dynamicCluster(
                          stripe(
                              server("server-1", IpUtils.getHostName())
                                  .configRepo("terracotta1/repository")
                                  .logs("terracotta1/logs")
                                  .metaData("terracotta1/metadata")
                                  .failoverPriority("availability")
                          )
                      )
                  )
              )
          ), true, true);

  @After
  public void close() throws Exception {
    try {
      angelaRule.close();
    } finally {
      IOUtils.closeQuietly(angelaOrchestrator, portAllocator);
    }
  }

  @Test
  public void testNodeStartup() {
    Tsa tsa = angelaRule.tsa();
    waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().size(), is(1));
    waitFor(() -> tsa.getTsaConfigurationContext().getTopology().getStripes().get(0).size(), is(1));
    waitFor(() -> tsa.getStarted().size(), is(1));
  }

  private void waitFor(Callable<Integer> condition, Matcher<Integer> matcher) {
    await().atMost(Duration.ofSeconds(60)).pollInterval(Duration.ofSeconds(1)).until(condition, matcher);
  }
}
