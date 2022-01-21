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
package org.terracotta.angela.agent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteException;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.logger.NullLogger;
import org.apache.ignite.logger.slf4j.Slf4jLogger;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.angela.common.AngelaProperties;
import org.terracotta.angela.common.net.DefaultPortAllocator;
import org.terracotta.angela.common.util.AngelaVersion;
import org.terracotta.angela.common.util.IgniteCommonHelper;
import org.terracotta.angela.common.util.IpUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.terracotta.angela.common.AngelaProperties.DIRECT_JOIN;
import static org.terracotta.angela.common.AngelaProperties.IGNITE_LOGGING;
import static org.terracotta.angela.common.AngelaProperties.NODE_NAME;
import static org.terracotta.angela.common.AngelaProperties.getEitherOf;
import static org.terracotta.angela.common.util.FileUtils.createAndValidateDir;

/**
 * @author Ludovic Orban
 */
public class Agent {

  public static final String AGENT_IS_READY_MARKER_LOG = "Agent is ready";
  public static final Path ROOT_DIR;
  public static final Path WORK_DIR;
  public static final Path IGNITE_DIR;

  private static final Logger logger;

  private static volatile Agent instance;

  static {
    // the angela-agent jar may end up on the classpath, so its logback config cannot have the default filename
    // this must happen before any Logger instance gets created
    System.setProperty("logback.configurationFile", "angela-logback.xml");
    logger = LoggerFactory.getLogger(Agent.class);

    ROOT_DIR = Paths.get(getEitherOf(AngelaProperties.ROOT_DIR, AngelaProperties.KITS_DIR));
    if (!ROOT_DIR.isAbsolute()) {
      throw new IllegalArgumentException("Expected ROOT_DIR to be an absolute path, got: " + ROOT_DIR);
    }
    WORK_DIR = ROOT_DIR.resolve("work");
    IGNITE_DIR = ROOT_DIR.resolve("ignite");
  }

  private final AgentController controller;
  private final Ignite ignite;
  private final int igniteDiscoveryPort;
  private final int igniteComPort;

  private Agent(AgentController controller, Ignite ignite, int igniteDiscoveryPort, int igniteComPort) {
    this.controller = controller;
    this.ignite = ignite;
    this.igniteDiscoveryPort = igniteDiscoveryPort;
    this.igniteComPort = igniteComPort;
  }

  @Override
  public String toString() {
    return ignite == null ? "local" : (IpUtils.getHostName() + ":" + igniteDiscoveryPort);
  }

  public static void main(String[] args) {
    int igniteDiscoveryPort = Integer.parseInt(System.getProperty("ignite.discovery.port"));
    int igniteComPort = Integer.parseInt(System.getProperty("ignite.com.port"));
    Agent agent = startCluster(Arrays.asList(DIRECT_JOIN.getValue().split(",")), NODE_NAME.getValue(), igniteDiscoveryPort, igniteComPort);
    Agent.setUniqueInstance(agent);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> Agent.getInstance().close()));
  }

  public static Agent startLocalCluster() {
    logger.info("Root directory is: {}", ROOT_DIR);
    logger.info("Starting local only cluster");
    createAndValidateDir(ROOT_DIR);
    createAndValidateDir(WORK_DIR);

    Agent agent = new Agent(new LocalAgentController(new DefaultPortAllocator()), null, 0, 0);

    // Do not use logger here as the marker is being grep'ed at and we do not want to depend upon the logger config
    System.out.println(AGENT_IS_READY_MARKER_LOG);
    System.out.flush();

    return agent;
  }

  public static Agent startCluster(Collection<String> peers, String nodeName, int igniteDiscoveryPort, int igniteComPort) {
    logger.info("Root directory is: {}", ROOT_DIR);
    logger.info("Nodename: {} added to cluster", nodeName);
    createAndValidateDir(ROOT_DIR);
    createAndValidateDir(WORK_DIR);
    createAndValidateDir(IGNITE_DIR);

    IgniteConfiguration cfg = new IgniteConfiguration();
    Map<String, String> userAttributes = new HashMap<>();
    userAttributes.put("angela.version", AngelaVersion.getAngelaVersion());
    userAttributes.put("nodename", nodeName);
    cfg.setUserAttributes(userAttributes);

    boolean enableLogging = Boolean.getBoolean(IGNITE_LOGGING.getValue());
    cfg.setGridLogger(enableLogging ? new Slf4jLogger() : new NullLogger());
    cfg.setPeerClassLoadingEnabled(true);
    cfg.setMetricsLogFrequency(0);
    cfg.setIgniteInstanceName("ignite-" + igniteDiscoveryPort);
    cfg.setIgniteHome(IGNITE_DIR.resolve(System.getProperty("user.name")).toString());

    logger.info("Connecting to peers (size = {}): {}", peers.size(), peers);

    cfg.setDiscoverySpi(new TcpDiscoverySpi()
        .setLocalPort(igniteDiscoveryPort)
        .setLocalPortRange(0) // we must not use the range otherwise Ignite might bind to a port not reserved
        .setJoinTimeout(10000)
        .setIpFinder(new TcpDiscoveryVmIpFinder(true).setAddresses(peers)));

    cfg.setCommunicationSpi(new TcpCommunicationSpi()
        .setLocalPort(igniteComPort)
        .setLocalPortRange(0)); // we must not use the range otherwise Ignite might bind to a port not reserved

    Ignite ignite;
    try {
      logger.info("Starting ignite on {}", nodeName);
      ignite = Ignition.start(cfg);
      IgniteCommonHelper.displayCluster(ignite);

    } catch (IgniteException e) {
      throw new RuntimeException("Error starting node " + nodeName, e);
    }
    Agent agent = new Agent(new AgentController(ignite, peers, igniteDiscoveryPort, new DefaultPortAllocator()), ignite, igniteDiscoveryPort, igniteComPort);

    // Do not use logger here as the marker is being grep'ed at and we do not want to depend upon the logger config
    System.out.println(AGENT_IS_READY_MARKER_LOG);
    System.out.flush();

    return agent;
  }

  public int getIgniteDiscoveryPort() {
    return igniteDiscoveryPort;
  }

  public int getIgniteComPort() {
    return igniteComPort;
  }

  public Ignite getIgnite() {
    return ignite;
  }

  public AgentController getController() {
    return controller;
  }

  @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
  public void close() {
    if (ignite != null) {
      ignite.close();
    }
  }

  public static synchronized Agent getInstance() {
    if (instance == null) {
      throw new IllegalStateException("Agent not initialized");
    }
    return instance;
  }

  public static synchronized void setUniqueInstance(Agent agent) {
    if (Agent.instance != null) {
      throw new IllegalStateException("Agent already initialized to: " + Agent.instance);
    }
    logger.info("Installing agent: " + agent);
    Agent.instance = agent;
  }

  public static synchronized void removeUniqueInstance(Agent agent) {
    if (Agent.instance == null) {
      throw new IllegalStateException("Agent not initialized");
    }
    if (Agent.instance != agent) {
      throw new IllegalStateException("Unable to remove installed agent " + Agent.instance + ": caller has another agent " + agent);
    }
    logger.info("Uninstalling agent: " + agent);
    Agent.instance = null;
  }
}
