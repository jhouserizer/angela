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
package org.terracotta.angela.client;

import org.apache.ignite.Ignite;
import org.apache.ignite.lang.IgniteCallable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.angela.agent.Agent;
import org.terracotta.angela.agent.kit.LocalKitManager;
import org.terracotta.angela.client.config.TmsConfigurationContext;
import org.terracotta.angela.client.filesystem.RemoteFolder;
import org.terracotta.angela.client.util.IgniteClientHelper;
import org.terracotta.angela.common.TerracottaCommandLineEnvironment;
import org.terracotta.angela.common.TerracottaManagementServerState;
import org.terracotta.angela.common.distribution.Distribution;
import org.terracotta.angela.common.tcconfig.License;
import org.terracotta.angela.common.tms.security.config.TmsClientSecurityConfig;
import org.terracotta.angela.common.tms.security.config.TmsServerSecurityConfig;
import org.terracotta.angela.common.topology.InstanceId;
import org.terracotta.angela.common.util.HostPort;

import java.util.Collections;
import java.util.Map;

import static org.terracotta.angela.common.AngelaProperties.KIT_INSTALLATION_DIR;
import static org.terracotta.angela.common.AngelaProperties.KIT_INSTALLATION_PATH;
import static org.terracotta.angela.common.AngelaProperties.OFFLINE;
import static org.terracotta.angela.common.AngelaProperties.SKIP_UNINSTALL;
import static org.terracotta.angela.common.AngelaProperties.getEitherOf;

public class Tms implements AutoCloseable {

  private final static Logger logger = LoggerFactory.getLogger(Tsa.class);
  private final int ignitePort;
  private final TmsConfigurationContext tmsConfigurationContext;
  private boolean closed = false;
  private final Ignite ignite;
  private final InstanceId instanceId;
  private final LocalKitManager localKitManager;

  @Deprecated
  private static final String NONE = "none";
  @Deprecated
  private static final String BROWSER_SECURITY = "browser-security";
  @Deprecated
  private static final String CLUSTER_SECURITY = "cluster-security";
  @Deprecated
  public static final String FULL = "full";

  Tms(Ignite ignite, int ignitePort, InstanceId instanceId, TmsConfigurationContext tmsConfigurationContext) {
    this.ignitePort = ignitePort;
    this.tmsConfigurationContext = tmsConfigurationContext;
    this.instanceId = instanceId;
    this.ignite = ignite;
    this.localKitManager = new LocalKitManager(tmsConfigurationContext.getDistribution());
    install();
  }

  public TmsConfigurationContext getTmsConfigurationContext() {
    return tmsConfigurationContext;
  }

  public String url() {
    boolean isHttps = false;
    TmsServerSecurityConfig tmsServerSecurityConfig = tmsConfigurationContext.getSecurityConfig();
    if (tmsServerSecurityConfig != null) {
      isHttps = ("true".equals(tmsServerSecurityConfig.getTmsSecurityHttpsEnabled())
                 || FULL.equals(tmsServerSecurityConfig.getDeprecatedSecurityLevel())
                 || BROWSER_SECURITY.equals(tmsServerSecurityConfig.getDeprecatedSecurityLevel())
      );
    }
    return (isHttps ? "https://" : "http://") + new HostPort(tmsConfigurationContext.getHostname(), 9480).getHostPort();
  }

  public TmsHttpClient httpClient() {
    return httpClient(null);
  }

  public TmsHttpClient httpClient(TmsClientSecurityConfig tmsClientSecurityConfig) {
    return new TmsHttpClient(url(), tmsClientSecurityConfig);
  }

  public RemoteFolder browse(String root) {
    String tmsHostname = tmsConfigurationContext.getHostname();
    String path = IgniteClientHelper.executeRemotely(ignite, tmsHostname, ignitePort, () -> Agent.getInstance().getController().getTmsInstallationPath(instanceId));
    return new RemoteFolder(ignite, tmsHostname, ignitePort, path, root);
  }

  public TerracottaManagementServerState getTmsState() {
    return IgniteClientHelper.executeRemotely(ignite, tmsConfigurationContext.getHostname(), ignitePort,
        () -> Agent.getInstance().getController().getTmsState(instanceId));
  }

  public Tms start() {
    return start(Collections.emptyMap());
  }

  public Tms start(Map<String, String> envOverrides) {
    String tmsHostname = tmsConfigurationContext.getHostname();
    logger.info("Starting TMS on {}", tmsHostname);
    IgniteClientHelper.executeRemotely(ignite, tmsHostname, ignitePort, () -> Agent.getInstance().getController().startTms(instanceId, envOverrides));
    return this;
  }

  public void stop() {
    stopTms(tmsConfigurationContext.getHostname());
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;

    try {
      stop();
    } catch (Exception e) {
      e.printStackTrace();
      // ignore, not installed
    }
    if (!SKIP_UNINSTALL.getBooleanValue()) {
      uninstall();
    }
  }

  private void uninstall() {
    String tmsHostname = tmsConfigurationContext.getHostname();
    TerracottaManagementServerState terracottaServerState = getTmsState();
    if (terracottaServerState == null) {
      return;
    }
    if (terracottaServerState != TerracottaManagementServerState.STOPPED) {
      throw new IllegalStateException("Cannot uninstall: server " + tmsHostname + " already in state " + terracottaServerState);
    }

    logger.info("Uninstalling TMS from {}", tmsHostname);
    String kitInstallationPath = getEitherOf(KIT_INSTALLATION_DIR, KIT_INSTALLATION_PATH);
    IgniteClientHelper.executeRemotely(ignite, tmsHostname, ignitePort,
        () -> Agent.getInstance().getController().uninstallTms(instanceId, tmsConfigurationContext.getDistribution(),
            tmsConfigurationContext.getSecurityConfig(),
            localKitManager.getKitInstallationName(), tmsHostname, kitInstallationPath));
  }

  private void install() {
    String tmsHostname = tmsConfigurationContext.getHostname();
    License license = tmsConfigurationContext.getLicense();
    Distribution distribution = tmsConfigurationContext.getDistribution();
    TmsServerSecurityConfig tmsServerSecurityConfig = tmsConfigurationContext.getSecurityConfig();
    TerracottaCommandLineEnvironment tcEnv = tmsConfigurationContext.getTerracottaCommandLineEnvironment();

    logger.info("starting TMS on {}", tmsHostname);

    logger.debug("Setting up locally the extracted install to be deployed remotely");
    String kitInstallationPath = getEitherOf(KIT_INSTALLATION_DIR, KIT_INSTALLATION_PATH);
    localKitManager.setupLocalInstall(license, kitInstallationPath, OFFLINE.getBooleanValue());

    logger.info("Attempting to remotely install if distribution already exists on {}", tmsHostname);
    IgniteCallable<Boolean> callable = () -> Agent.getInstance().getController().installTms(instanceId, tmsHostname, distribution, license,
        tmsServerSecurityConfig, localKitManager.getKitInstallationName(), tcEnv, tmsConfigurationContext.getHostname(), kitInstallationPath);
    boolean isRemoteInstallationSuccessful = IgniteClientHelper.executeRemotely(ignite, tmsHostname, ignitePort, callable);

    if (!isRemoteInstallationSuccessful) {
      try {
        IgniteClientHelper.uploadKit(ignite, tmsHostname, ignitePort, instanceId, distribution,
            localKitManager.getKitInstallationName(), localKitManager.getKitInstallationPath().toFile());
        IgniteClientHelper.executeRemotely(ignite, tmsHostname, ignitePort, callable);
      } catch (Exception e) {
        throw new RuntimeException("Cannot upload kit to " + tmsHostname, e);
      }
    }

  }

  private void stopTms(String tmsHostname) {
    TerracottaManagementServerState terracottaManagementServerState = getTmsState();
    if (terracottaManagementServerState == TerracottaManagementServerState.STOPPED) {
      return;
    }
    if (terracottaManagementServerState != TerracottaManagementServerState.STARTED) {
      throw new IllegalStateException("Cannot stop: TMS server , already in state " + terracottaManagementServerState);
    }

    logger.info("Stopping TMS on {}", tmsHostname);
    IgniteClientHelper.executeRemotely(ignite, tmsHostname, ignitePort, () -> Agent.getInstance().getController().stopTms(instanceId));
  }

}
