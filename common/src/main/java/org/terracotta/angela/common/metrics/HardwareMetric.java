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
package org.terracotta.angela.common.metrics;

public enum HardwareMetric {

    CPU(new MonitoringCommand("mpstat",
        "-P", // Specify the processors
        "ALL", // Specify ALL processors - duh!
        "10")),
    DISK(new MonitoringCommand( "iostat",
        "-h", // Human-readable output
        "-d", // Record stats for disks
        "10")),
    MEMORY(new MonitoringCommand("free",
        "-h", // Human-readable output
        "-s", // Specify collection time in seconds
        "10" )),
    NETWORK(new MonitoringCommand("sar",
        "-n", // Specify network statistics
        "DEV", // Observe traffic on interfaces
        "10")),
    ;

    private final MonitoringCommand defaultMonitoringCommand;

    HardwareMetric(MonitoringCommand defaultMonitoringCommand) {
        this.defaultMonitoringCommand = defaultMonitoringCommand;
    }

    public MonitoringCommand getDefaultMonitoringCommand() {
        return defaultMonitoringCommand;
    }

}
