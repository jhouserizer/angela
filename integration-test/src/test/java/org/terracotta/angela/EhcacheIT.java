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

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.clustered.client.config.builders.ClusteredResourcePoolBuilder;
import org.ehcache.clustered.client.config.builders.ClusteringServiceConfigurationBuilder;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.junit.Test;
import org.terracotta.angela.client.ClientArray;
import org.terracotta.angela.client.ClientArrayFuture;
import org.terracotta.angela.client.ClientJob;
import org.terracotta.angela.client.ClusterFactory;
import org.terracotta.angela.client.Tsa;
import org.terracotta.angela.client.config.ConfigurationContext;
import org.terracotta.angela.client.config.custom.CustomConfigurationContext;
import org.terracotta.angela.common.tcconfig.TcConfig;
import org.terracotta.angela.common.topology.ClientArrayTopology;
import org.terracotta.angela.common.topology.Topology;
import org.terracotta.angela.util.TestUtils;

import java.net.URI;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.terracotta.angela.common.clientconfig.ClientArrayConfig.newClientArrayConfig;
import static org.terracotta.angela.common.tcconfig.TcConfig.tcConfig;
import static org.terracotta.angela.common.topology.Version.version;
import static org.terracotta.angela.util.Versions.EHCACHE_VERSION_XML;

public class EhcacheIT extends BaseIT {

  public EhcacheIT(String mode, String hostname, boolean inline, boolean ssh) {
    super(mode, hostname, inline, ssh);
  }

  @Test
  public void testTsaWithEhcacheReleaseKit() throws Exception {
    ConfigurationContext configContext = CustomConfigurationContext.customConfigurationContext()
        .tsa(
            tsa -> tsa.topology(
                new Topology(
                    getOldDistribution(),
                    tcConfig(version(EHCACHE_VERSION_XML), TestUtils.TC_CONFIG_A)
                )
            )
        );

    try (ClusterFactory factory = angelaOrchestrator.newClusterFactory("EhcacheTest::testTsaWithEhcacheReleaseKit", configContext)) {
      factory.tsa().startAll();
    }
  }

  @Test
  public void testClusteredEhcacheOperations() throws Exception {
    assumeFalse("Cannot run without Ignite when using client jobs", agentID.isIgniteFree());
    assumeTrue("Cannot run through local SSH using a fake host file", sshServer == null);

    TcConfig tcConfig = tcConfig(version(EHCACHE_VERSION_XML), TestUtils.TC_CONFIG_A);
    tcConfig.updateServerHost(0, hostname);

    ConfigurationContext configContext = CustomConfigurationContext.customConfigurationContext()
        .tsa(
            tsa -> tsa.topology(
                new Topology(
                    getOldDistribution(),
                    tcConfig
                )
            )
        ).clientArray(
            clientArray -> clientArray.clientArrayTopology(
                new ClientArrayTopology(
                    getOldDistribution(),
                    newClientArrayConfig().host("foo", hostname)
                )
            )
        );

    try (ClusterFactory factory = angelaOrchestrator.newClusterFactory("EhcacheTest::testClusteredEhcacheOperations", configContext)) {
      Tsa tsa = factory.tsa();
      tsa.startAll();
      tsa.waitForActive();
      String uri = tsa.uri().toString() + "/clustered-cache-manager";
      ClientArray clientArray = factory.clientArray(0);
      String cacheAlias = "clustered-cache";

      ClientJob clientJob = (cluster) -> {
        try (CacheManager cacheManager = createCacheManager(uri, cacheAlias)) {
          Cache<Long, String> cache = cacheManager.getCache(cacheAlias, Long.class, String.class);
          final int numKeys = 10;
          for (long key = 0; key < numKeys; key++) {
            cache.put(key, String.valueOf(key) + key);
          }

          for (long key = 0; key < numKeys; key++) {
            assertEquals(cache.get(key), String.valueOf(key) + key);
          }
        }
      };

      ClientArrayFuture caf = clientArray.executeOnAll(clientJob);
      caf.get();
    }
  }

  private static CacheManager createCacheManager(String uri, String cacheAlias) {
    return CacheManagerBuilder
        .newCacheManagerBuilder()
        .with(ClusteringServiceConfigurationBuilder
            .cluster(URI.create(uri))
            .autoCreate(s -> s.defaultServerResource("main").resourcePool("resource-pool-a", 10, MemoryUnit.MB))
        ).withCache(cacheAlias, CacheConfigurationBuilder.newCacheConfigurationBuilder(
                Long.class,
                String.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .heap(1000, EntryUnit.ENTRIES)
                    .offheap(1, MemoryUnit.MB)
                    .with(ClusteredResourcePoolBuilder.clusteredShared("resource-pool-a"))
            )
        ).build(true);
  }
}