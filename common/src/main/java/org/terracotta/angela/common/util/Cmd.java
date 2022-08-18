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
package org.terracotta.angela.common.util;

import org.terracotta.angela.common.ToolExecutionResult;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Cmd {
  public static ToolExecutionResult cmd(File workingDir, String terracottaCommand, String[] arguments) {
    Path path = Paths.get(workingDir.getAbsolutePath(), terracottaCommand + OS.INSTANCE.getShellExtension());
    List<String> cmdLine = new ArrayList<>();
    cmdLine.add(path.toAbsolutePath().toString());
    Collections.addAll(cmdLine, arguments);

    try {
      ProcessResult processResult = new ProcessExecutor(cmdLine)
          .readOutput(true)
          .redirectErrorStream(true)
          .execute();
      return new ToolExecutionResult(processResult.getExitValue(), processResult.getOutput().getLines());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}