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
package org.terracotta.angela.client.config.custom;

import org.terracotta.angela.client.config.MonitoringConfigurationContext;
import org.terracotta.angela.common.metrics.HardwareMetric;
import org.terracotta.angela.common.metrics.MonitoringCommand;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class CustomMonitoringConfigurationContext implements MonitoringConfigurationContext {
  private final Map<HardwareMetric, MonitoringCommand> commands = new HashMap<>();

  @Override
  public Map<HardwareMetric, MonitoringCommand> commands() {
    return Collections.unmodifiableMap(commands);
  }

  public CustomMonitoringConfigurationContext commands(EnumSet<HardwareMetric> hardwareMetrics) {
    for (HardwareMetric hardwareMetric : hardwareMetrics) {
      commands.put(hardwareMetric, hardwareMetric.getDefaultMonitoringCommand());
    }
    return this;
  }

  public CustomMonitoringConfigurationContext command(HardwareMetric hardwareMetric, MonitoringCommand monitoringCommand) {
    commands.put(hardwareMetric, monitoringCommand);
    return this;
  }

}
