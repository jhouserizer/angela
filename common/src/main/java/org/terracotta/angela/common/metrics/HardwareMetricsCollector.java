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
package org.terracotta.angela.common.metrics;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.angela.common.util.ProcessUtil;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.process.PidUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Aurelien Broszniowski
 */

public class HardwareMetricsCollector {
  private final static Logger LOGGER = LoggerFactory.getLogger(HardwareMetricsCollector.class);
  public final static String METRICS_DIRECTORY = "metrics";

  private FileOutputStream outputStream;
  private final Map<HardwareMetric, StartedProcess> processes = new HashMap<>();

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
  public void startMonitoring(final File installLocation, final Map<HardwareMetric, MonitoringCommand> commands) {
    if (commands != null && commands.size() != 0) {
      File statsDirectory = new File(installLocation, METRICS_DIRECTORY);
      statsDirectory.mkdirs();

      commands.forEach((hardwareMetric, command) -> {
        File statsFile = new File(statsDirectory, hardwareMetric.name().toLowerCase() + "-stats.log");
        LOGGER.debug("HardwareMetric log file: {}", statsFile.getAbsolutePath());
        try {
          outputStream = new FileOutputStream(statsFile);
        } catch (FileNotFoundException e) {
          throw new RuntimeException(e);
        }

        ProcessExecutor pe = new ProcessExecutor()
            .environment(System.getenv())
            .command(command.getCommand())
            .directory(installLocation)
            .redirectErrorStream(true)
            .redirectOutput(outputStream);

        try {
          LOGGER.debug("Starting process with env: {}", pe.getEnvironment());
          processes.put(hardwareMetric, pe.start());
        } catch (IOException e) {
          try {
            Files.write(statsFile.toPath(), ("Error executing command '" + command.getCommandName() + "': " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
          } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
          }
        }
      });
    }
  }

  public boolean isMonitoringRunning(HardwareMetric hardwareMetric) {
    final StartedProcess process = processes.get(hardwareMetric);
    if (process == null) {
      return false; // No process was found - i.e. it failed at startup
    } else {
      return process.getProcess().isAlive();
    }
  }

  public void stopMonitoring() {
    List<Exception> exceptions = new ArrayList<>();

    processes.values().forEach(process -> {
      try {
        ProcessUtil.destroyGracefullyOrForcefullyAndWait(PidUtil.getPid(process.getProcess()));
      } catch (Exception e) {
        exceptions.add(e);
      }
    });

    processes.clear();

    if (this.outputStream != null) {
      try {
        this.outputStream.close();
      } catch (IOException e) {
        exceptions.add(e);
      }
      this.outputStream = null;
    }

    if (!exceptions.isEmpty()) {
      RuntimeException runtimeException = new RuntimeException();
      exceptions.forEach(runtimeException::addSuppressed);
      throw runtimeException;
    }
  }
}
