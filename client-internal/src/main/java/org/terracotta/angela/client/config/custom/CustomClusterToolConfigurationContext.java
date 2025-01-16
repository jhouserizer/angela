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

import org.terracotta.angela.client.config.ToolConfigurationContext;
import org.terracotta.angela.common.TerracottaClusterTool;
import org.terracotta.angela.common.TerracottaCommandLineEnvironment;
import org.terracotta.angela.common.distribution.Distribution;
import org.terracotta.angela.common.tcconfig.License;
import org.terracotta.angela.common.tcconfig.SecurityRootDirectory;

import java.nio.file.Path;

public class CustomClusterToolConfigurationContext implements ToolConfigurationContext {
  private TerracottaCommandLineEnvironment commandLineEnv = TerracottaCommandLineEnvironment.DEFAULT;
  private TerracottaClusterTool terracottaClusterTool;
  private SecurityRootDirectory securityRootDirectory;
  private Distribution distribution;
  private License license;

  protected CustomClusterToolConfigurationContext() {
  }

  public CustomClusterToolConfigurationContext clusterTool(TerracottaClusterTool terracottaClusterTool) {
    this.terracottaClusterTool = terracottaClusterTool;
    return this;
  }

  public CustomClusterToolConfigurationContext securityRootDirectory(Path securityDir) {
    this.securityRootDirectory = SecurityRootDirectory.securityRootDirectory(securityDir);
    return this;
  }

  public CustomClusterToolConfigurationContext distribution(Distribution distribution) {
    this.distribution = distribution;
    return this;
  }

  public CustomClusterToolConfigurationContext license(License license) {
    this.license = license;
    return this;
  }

  public CustomClusterToolConfigurationContext commandLineEnv(TerracottaCommandLineEnvironment commandLineEnv) {
    this.commandLineEnv = commandLineEnv;
    return this;
  }

  @Override
  public Distribution getDistribution() {
    return distribution;
  }

  @Override
  public License getLicense() {
    return license;
  }

  @Override
  public TerracottaCommandLineEnvironment getCommandLineEnv() {
    return commandLineEnv;
  }

  @Override
  public SecurityRootDirectory getSecurityRootDirectory() {
    return securityRootDirectory;
  }

  @Override
  public String getHostName() {
    return terracottaClusterTool.getHostName();
  }

  public TerracottaClusterTool getTerracottaClusterTool() {
    return terracottaClusterTool;
  }
}
