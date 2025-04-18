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
package org.terracotta.angela.common.metrics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MonitoringCommand implements Serializable {
  private static final long serialVersionUID = 1L;

  private final List<String> command;

  public MonitoringCommand(String... cmdArgs) {
    this(Arrays.asList(cmdArgs));
  }

  public MonitoringCommand(List<String> cmdArgs) {
    this.command = new ArrayList<>(cmdArgs);
  }

  public String getCommandName() {
    return command.get(0);
  }

  public List<String> getCommand() {
    return Collections.unmodifiableList(command);
  }

}
