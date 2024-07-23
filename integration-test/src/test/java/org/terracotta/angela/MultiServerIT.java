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

import com.terracotta.connection.api.DiagnosticConnectionService;
import com.terracotta.diagnostic.Diagnostics;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.angela.client.ClusterFactory;
import org.terracotta.angela.client.Tsa;
import org.terracotta.angela.client.config.ConfigurationContext;
import org.terracotta.angela.client.config.custom.CustomConfigurationContext;
import org.terracotta.angela.client.net.ServerToServerDisruptor;
import org.terracotta.angela.client.net.SplitCluster;
import org.terracotta.angela.common.tcconfig.TcConfig;
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.angela.common.topology.Topology;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionService;
import org.terracotta.connection.entity.EntityRef;

import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertTrue;
import static org.terracotta.angela.common.TerracottaServerState.STARTED_AS_ACTIVE;
import static org.terracotta.angela.common.TerracottaServerState.STARTED_AS_PASSIVE;
import static org.terracotta.angela.common.tcconfig.TcConfig.tcConfig;
import static org.terracotta.angela.common.topology.Version.version;
import static org.terracotta.angela.util.Versions.EHCACHE_VERSION_XML;

public class MultiServerIT extends BaseIT {
  private static final Logger logger = LoggerFactory.getLogger(MultiServerIT.class);

  private static final int STATE_TIMEOUT = 60_000;
  private static final int STATE_POLL_INTERVAL = 200;

  public MultiServerIT(String mode, String hostname, boolean inline, boolean ssh) {
    super(mode, hostname, inline, ssh);
  }

  /**
   * Create partition between [active] & [passive1,passive2] in consistent mode and verify
   * state of servers.
   */
  @Test
  public void testPartitionBetweenActivePassives() throws Exception {
    TcConfig tcConfig = tcConfig(version(EHCACHE_VERSION_XML), getClass().getResource("/configs/tc-config-app-consistent.xml"));
    tcConfig.updateServerHost(0, hostname);
    tcConfig.updateServerHost(1, hostname);
    tcConfig.updateServerHost(2, hostname);

    //set netDisruptionEnabled to true to enable disruption
    ConfigurationContext configContext = CustomConfigurationContext.customConfigurationContext()
        .tsa(tsa -> tsa.topology(new Topology(getOldDistribution(), true,
            tcConfig))
        );

    try (ClusterFactory factory = angelaOrchestrator.newClusterFactory("MultiServerTest::testPartitionBetweenActivePassives", configContext)) {
      try (Tsa tsa = factory.tsa().spawnAll()) {
        System.out.println(tcConfig.toXml());
        System.out.println(tcConfig.getServers());

        tsa.waitForActive();
        tsa.waitForPassives(2);

        TerracottaServer active = tsa.getActive();
        Collection<TerracottaServer> passives = tsa.getPassives();
        Iterator<TerracottaServer> iterator = passives.iterator();
        TerracottaServer passive1 = iterator.next();
        TerracottaServer passive2 = iterator.next();


        SplitCluster split1 = new SplitCluster(active);
        SplitCluster split2 = new SplitCluster(passives);

        //server to server disruption with active at one end and passives at other end.
        try (ServerToServerDisruptor disruptor = tsa.disruptionController().newServerToServerDisruptor(split1, split2)) {

          //start partition
          disruptor.disrupt();
          //verify active gets into blocked state and one of passives gets promoted to active
          assertTrue(waitForActive(tsa, passive1, passive2));
          assertTrue(waitForServerBlocked(active));

          //stop partition
          disruptor.undisrupt();
          //verify former active gets zapped and becomes passive after network restored
          assertTrue(waitForPassive(tsa, active));

        }
      }
    }
  }

  private static String getServerBlockedState(TerracottaServer server) throws Exception {
    ConnectionService connectionService = new DiagnosticConnectionService();
    URI uri = URI.create("diagnostic://" + server.getHostName() + ":" + server.getTsaPort());
    try (Connection connection = connectionService.connect(uri, new Properties())) {
      EntityRef<Diagnostics, Object, Void> ref = connection.getEntityRef(Diagnostics.class, 1, "root");
      try (Diagnostics diagnostics = ref.fetchEntity(null)) {
        return diagnostics.invoke("ConsistencyManager", "isBlocked");
      }
    }
  }

  private static boolean isServerBlocked(TerracottaServer server) {
    try {
      return Boolean.parseBoolean(getServerBlockedState(server));
    } catch (Exception e) {
      logger.warn("isServerBlocked({}): {}", server, e.getMessage(), e);
      return false;
    }
  }

  private static boolean waitForServerBlocked(TerracottaServer server) throws Exception {
    long endTime = System.currentTimeMillis() + STATE_TIMEOUT;
    while (endTime > System.currentTimeMillis()) {
      if (isServerBlocked(server)) {
        return true;
      } else {
        Thread.sleep(STATE_POLL_INTERVAL);
      }
    }
    throw new TimeoutException("Timeout when waiting for server to become blocked");
  }

  private static boolean waitForActive(Tsa tsa, TerracottaServer... servers) throws Exception {
    long endTime = System.currentTimeMillis() + STATE_TIMEOUT;
    int activeIndex = -1;
    while (endTime > System.currentTimeMillis()) {
      //first make sure one of server becoming active and then check remaining servers for passive state
      for (int i = 0; i < servers.length; ++i) {
        if (tsa.getState(servers[i]) == STARTED_AS_ACTIVE) {
          activeIndex = i;
          break;
        }
      }
      if (activeIndex == -1) {
        Thread.sleep(STATE_POLL_INTERVAL);
      } else {
        TerracottaServer[] passives = ArrayUtils.remove(servers, activeIndex);
        return passives.length == 0 || waitForPassive(tsa, passives);
      }
    }
    throw new TimeoutException("Timeout when waiting for server to become active");
  }

  private static boolean waitForPassive(Tsa tsa, TerracottaServer... servers) throws Exception {
    long endTime = System.currentTimeMillis() + STATE_TIMEOUT;
    while (endTime > System.currentTimeMillis()) {
      for (TerracottaServer server : servers) {
        if (tsa.getState(server) != STARTED_AS_PASSIVE) {
          break;
        }
        return true;
      }
      Thread.sleep(STATE_POLL_INTERVAL);
    }
    throw new TimeoutException("Timeout when waiting for server to become passive");
  }
}
