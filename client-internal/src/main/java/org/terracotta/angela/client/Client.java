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
import org.apache.ignite.lang.IgniteRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.angela.agent.Agent;
import org.terracotta.angela.agent.kit.LocalKitManager;
import org.terracotta.angela.client.com.IgniteFutureAdapter;
import org.terracotta.angela.client.filesystem.RemoteFolder;
import org.terracotta.angela.client.util.IgniteClientHelper;
import org.terracotta.angela.common.TerracottaCommandLineEnvironment;
import org.terracotta.angela.common.clientconfig.ClientId;
import org.terracotta.angela.common.cluster.Cluster;
import org.terracotta.angela.common.topology.InstanceId;

import java.io.Closeable;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;

import static org.terracotta.angela.common.AngelaProperties.SKIP_UNINSTALL;
import static org.terracotta.angela.common.util.JavaBinaries.javaHome;
import static org.terracotta.angela.common.util.JavaBinaries.jdkHome;

/**
 * @author Ludovic Orban
 */
public class Client implements Closeable {

  private final static Logger logger = LoggerFactory.getLogger(Client.class);

  private final int ignitePort;
  private final InstanceId instanceId;
  private final ClientId clientId;
  private final Ignite ignite;
  private final int subClientPid;
  private boolean stopped = false;
  private boolean closed = false;


  Client(Ignite ignite, int ignitePort, InstanceId instanceId, ClientId clientId, TerracottaCommandLineEnvironment tcEnv, LocalKitManager localKitManager) {
    this.ignitePort = ignitePort;
    this.instanceId = instanceId;
    this.clientId = clientId;
    this.ignite = ignite;
    this.subClientPid = spawnSubClient(
        Objects.requireNonNull(tcEnv),
        Objects.requireNonNull(localKitManager)
    );
  }

  public ClientId getClientId() {
    return clientId;
  }

  int getPid() {
    return subClientPid;
  }

  private int spawnSubClient(TerracottaCommandLineEnvironment tcEnv, LocalKitManager localKitManager) {
    logger.info("Spawning client '{}' on {}", instanceId, clientId);

    try {
      IgniteClientHelper.uploadClientJars(ignite, getHostname(), ignitePort, instanceId, listClasspathFiles(localKitManager));

      IgniteCallable<Integer> igniteCallable = () -> Agent.getInstance().getController().spawnClient(instanceId, tcEnv);
      int pid = IgniteClientHelper.executeRemotely(ignite, getHostname(), ignitePort, igniteCallable);
      logger.info("client '{}' on {} started with PID {}", instanceId, clientId, pid);

      return pid;
    } catch (Exception e) {
      logger.error("Cannot create client on {}: {}", clientId, e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("StatementWithEmptyBody")
  private List<File> listClasspathFiles(LocalKitManager localKitManager) {
    List<File> files = new ArrayList<>();

    String javaHome = jdkHome().orElse(javaHome()).toString();
    logger.debug("Skipping all JVM libraries inside {}", javaHome);

    String[] classpathJarNames = System.getProperty("java.class.path").split(File.pathSeparator);
    boolean substituteClientJars = localKitManager.getDistribution() != null;
    List<File> libs = new ArrayList<>();

    for (String classpathJarName : classpathJarNames) {
      if (classpathJarName.startsWith(javaHome)) {
        logger.debug("Skipping {} as it is part of the JVM", classpathJarName);
        continue; // part of the JVM, skip it
      }
      File classpathFile = new File(classpathJarName);

      File equivalentClientJar = localKitManager.equivalentClientJar(classpathFile);
      if (substituteClientJars && equivalentClientJar != null) {
        logger.debug("Skipping upload of classpath file as kit contains equivalent jar in client libs : {}", classpathFile.getName());
        libs.add(equivalentClientJar);
        continue;
      }

      logger.debug("Uploading classpath file : {}", classpathFile.getName());
      files.add(classpathFile);
    }

    if (substituteClientJars) {
      logger.info("Enhancing client classpath with client jars of {}", localKitManager.getDistribution());
      files.addAll(libs);
      logger.debug("Adding clients jars : {}", libs);
    }

    return files;
  }

  Future<Void> submit(ClientId clientId, ClientJob clientJob) {
    IgniteCallable<Void> call = () -> {
      try {
        clientJob.run(new Cluster(ignite, clientId));
        return null;
      } catch (Throwable t) {
        throw new IgniteFutureAdapter.RemoteExecutionException("Remote ClientJob failed", exceptionToString(t));
      }
    };
    return IgniteClientHelper.executeRemotelyAsync(ignite, instanceId.toString(), ignitePort, call);
  }

  private static String exceptionToString(Throwable t) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    pw.close();
    return sw.toString();
  }

  public RemoteFolder browse(String root) {
    return new RemoteFolder(ignite, instanceId.toString(), ignitePort, null, root);
  }

  public InstanceId getInstanceId() {
    return instanceId;
  }

  public String getHostname() {
    return clientId.getHostName();
  }

  public String getSymbolicName() {
    return clientId.getSymbolicName().getSymbolicName();
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;

    stop();
    if (!SKIP_UNINSTALL.getBooleanValue()) {
      logger.info("Wiping up client '{}' on {}", instanceId, clientId);
      IgniteClientHelper.executeRemotely(ignite, getHostname(), ignitePort, (IgniteRunnable) () -> Agent.getInstance().getController().deleteClient(instanceId));
    }
  }

  public void stop() {
    if (stopped) {
      return;
    }
    stopped = true;

    logger.info("Killing client '{}' on {}", instanceId, clientId);
    IgniteClientHelper.executeRemotely(ignite, getHostname(), ignitePort, (IgniteRunnable) () -> Agent.getInstance().getController().stopClient(instanceId, subClientPid));
  }
}
