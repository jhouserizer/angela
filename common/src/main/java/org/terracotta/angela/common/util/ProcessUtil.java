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
package org.terracotta.angela.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.process.Processes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ProcessUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProcessUtil.class);
  private static final Duration GRACEFUL_TIMEOUT = Duration.ofSeconds(30);
  private static final Duration FORCEFUL_DELAY = Duration.ofSeconds(10);

  public static void destroyGracefullyOrForcefullyAndWait(int pid) throws IOException, InterruptedException, TimeoutException {
    if (OS.INSTANCE.isWindows()) {
      destroyOnWindows(pid);
    } else {
      org.zeroturnaround.process.ProcessUtil.destroyGracefullyOrForcefullyAndWait(Processes.newPidProcess(pid),
          GRACEFUL_TIMEOUT.toSeconds(), TimeUnit.SECONDS,
          FORCEFUL_DELAY.toSeconds(), TimeUnit.SECONDS);
    }
  }

  private static void destroyOnWindows(int pid) throws IOException, InterruptedException, TimeoutException {
    long deadline = System.nanoTime() + GRACEFUL_TIMEOUT.toNanos();

    runPowerShell("Stop-Process -Id " + pid + " -ErrorAction SilentlyContinue");
    if (waitForExitWindows(pid, deadline - FORCEFUL_DELAY.toNanos())) {
      return;
    }

    LOGGER.debug("Process {} still alive after graceful stop, forcing termination", pid);
    runPowerShell("Stop-Process -Id " + pid + " -Force -ErrorAction SilentlyContinue");
    if (!waitForExitWindows(pid, deadline - System.nanoTime())) {
      throw new TimeoutException("Timed out waiting for process " + pid + " to terminate");
    }
  }

  private static boolean waitForExitWindows(int pid, long remainingNanos) throws IOException, InterruptedException {
    long deadline = System.nanoTime() + Math.max(0, remainingNanos);
    while (System.nanoTime() < deadline) {
      if (!isProcessAliveWindows(pid)) {
        return true;
      }
      Thread.sleep(500);
    }
    return !isProcessAliveWindows(pid);
  }

  private static boolean isProcessAliveWindows(int pid) throws IOException, InterruptedException {
    String command = "$p = Get-Process -Id " + pid + " -ErrorAction SilentlyContinue; if ($p) { exit 0 } else { exit 1 }";
    return runPowerShell(command) == 0;
  }

  private static int runPowerShell(String command) throws IOException, InterruptedException {
    ProcessBuilder builder = new ProcessBuilder("powershell.exe", "-NoLogo", "-NoProfile", "-Command", command);
    Process process = builder.start();
    byte[] output = process.getInputStream().readAllBytes();
    byte[] error = process.getErrorStream().readAllBytes();
    int exitCode = process.waitFor();
    if (exitCode != 0 && LOGGER.isDebugEnabled()) {
      LOGGER.debug("PowerShell command '{}' failed with code {}. Out: {} Err: {}", command, exitCode,
          new String(output, StandardCharsets.UTF_8), new String(error, StandardCharsets.UTF_8));
    }
    return exitCode;
  }
}
