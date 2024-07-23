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
package org.terracotta.angela.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.angela.agent.Agent;
import org.terracotta.angela.agent.AgentController;
import org.terracotta.angela.agent.com.AgentExecutor;
import org.terracotta.angela.agent.com.Executor;
import org.terracotta.angela.client.filesystem.RemoteFolder;
import org.terracotta.angela.client.filesystem.TransportableFile;
import org.terracotta.angela.common.metrics.HardwareMetric;
import org.terracotta.angela.common.metrics.HardwareMetricsCollector;
import org.terracotta.angela.common.metrics.MonitoringCommand;
import org.terracotta.angela.common.topology.InstanceId;
import org.terracotta.angela.common.util.UniversalPath;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * @author Aurelien Broszniowski
 */
@SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
public class ClusterMonitor implements AutoCloseable, Serializable {
  private static final long serialVersionUID = 1L;

  private static final Logger logger = LoggerFactory.getLogger(ClusterMonitor.class);

  private final InstanceId instanceId;
  private final transient Map<String, AgentExecutor> executors;
  private final Map<HardwareMetric, MonitoringCommand> commands;
  private boolean closed = false;

  ClusterMonitor(Executor executor, InstanceId instanceId, Set<String> hostnames, Map<HardwareMetric, MonitoringCommand> commands) {
    this.instanceId = instanceId;
    this.executors = hostnames.stream().collect(toMap(identity(), hostname -> executor.forAgent(executor.getAgentID(hostname))));
    this.commands = commands;
  }

  public ClusterMonitor startOnAll() {
    List<Exception> exceptions = new ArrayList<>();

    for (Map.Entry<String, AgentExecutor> entry : executors.entrySet()) {
      logger.info("Starting monitoring: {} on: {} with agent: {}", commands.keySet(), entry.getKey(), entry.getValue().getTarget());
      try {
        entry.getValue().execute(() -> AgentController.getInstance().startHardwareMonitoring(getWorkingPath(), commands));
      } catch (RuntimeException e) {
        exceptions.add(new RuntimeException("Error starting hardware monitoring on: " + entry.getValue().getTarget() + ". Err: " + e.getMessage(), e));
      }
    }

    if (!exceptions.isEmpty()) {
      RuntimeException re = new RuntimeException("Error starting cluster monitors");
      exceptions.forEach(re::addSuppressed);
      throw re;
    }
    return this;
  }

  public ClusterMonitor stopOnAll() {
    List<Exception> exceptions = new ArrayList<>();

    for (Map.Entry<String, AgentExecutor> entry : executors.entrySet()) {
      try {
        entry.getValue().execute(() -> AgentController.getInstance().stopHardwareMonitoring());
      } catch (Exception e) {
        exceptions.add(e);
      }
    }

    if (!exceptions.isEmpty()) {
      RuntimeException re = new RuntimeException("Error stopping cluster monitors");
      exceptions.forEach(re::addSuppressed);
      throw re;
    }
    return this;
  }

  public void downloadTo(File localPath) {
    downloadTo(localPath.toPath());
  }

  public void downloadTo(Path localPath) {
    List<Exception> exceptions = new ArrayList<>();

    for (Map.Entry<String, AgentExecutor> entry : executors.entrySet()) {
      try {
        // a way to grab a path remotely from an OS (win or lin) and transfer it locally
        UniversalPath fromRemote = entry.getValue().execute(() -> UniversalPath.fromLocalPath(getWorkingPath().resolve(HardwareMetricsCollector.METRICS_DIRECTORY)));
        Path toLocal = localPath.resolve(entry.getKey());
        logger.info("Downloading remote metrics from: {} to: {}", fromRemote, toLocal);
        new RemoteFolder(entry.getValue(), null, fromRemote.toString()).downloadTo(toLocal);
      } catch (IOException e) {
        exceptions.add(e);
      }
    }

    if (!exceptions.isEmpty()) {
      RuntimeException re = new RuntimeException("Error downloading cluster monitor remote files");
      exceptions.forEach(re::addSuppressed);
      throw re;
    }
  }

  public void processMetrics(BiConsumer<String, TransportableFile> processor) {
    List<Exception> exceptions = new ArrayList<>();
    for (Map.Entry<String, AgentExecutor> entry : executors.entrySet()) {
      try {
        // a way to grab a path remotely from an OS (win or lin) and transfer it locally
        UniversalPath metricsPath = entry.getValue().execute(() -> UniversalPath.fromLocalPath(getWorkingPath().resolve(HardwareMetricsCollector.METRICS_DIRECTORY)));
        RemoteFolder remoteFolder = new RemoteFolder(entry.getValue(), null, metricsPath.toString());
        remoteFolder.list().forEach(remoteFile -> processor.accept(entry.getKey(), remoteFile.toTransportableFile()));
      } catch (Exception e) {
        exceptions.add(e);
      }
    }

    if (!exceptions.isEmpty()) {
      RuntimeException re = new RuntimeException("Error downloading cluster monitor remote files");
      exceptions.forEach(re::addSuppressed);
      throw re;
    }
  }

  private Path getWorkingPath() {
    return Agent.WORK_DIR.resolve(instanceId.toString());
  }

  public boolean isMonitoringRunning(HardwareMetric metric) {
    for (Map.Entry<String, AgentExecutor> entry : executors.entrySet()) {
      boolean running = entry.getValue().execute(() -> AgentController.getInstance().isMonitoringRunning(metric));
      if (!running) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;

    stopOnAll();
  }

}
