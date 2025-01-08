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

import org.terracotta.angela.client.config.VoterConfigurationContext;
import org.terracotta.angela.common.TerracottaCommandLineEnvironment;
import org.terracotta.angela.common.TerracottaVoter;
import org.terracotta.angela.common.distribution.Distribution;
import org.terracotta.angela.common.tcconfig.License;
import org.terracotta.angela.common.tcconfig.SecurityRootDirectory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CustomVoterConfigurationContext implements VoterConfigurationContext {
  private final List<TerracottaVoter> terracottaVoters = new ArrayList<>();
  private TerracottaCommandLineEnvironment commandLineEnv = TerracottaCommandLineEnvironment.DEFAULT;
  private SecurityRootDirectory securityRootDirectory;
  private Distribution distribution;
  private License license;

  protected CustomVoterConfigurationContext() {
  }

  public CustomVoterConfigurationContext addVoter(TerracottaVoter terracottaVoter) {
    this.terracottaVoters.add(terracottaVoter);
    return this;
  }

  public CustomVoterConfigurationContext securityRootDirectory(Path securityDir) {
    this.securityRootDirectory = SecurityRootDirectory.securityRootDirectory(securityDir);
    return this;
  }

  public CustomVoterConfigurationContext distribution(Distribution distribution) {
    this.distribution = distribution;
    return this;
  }

  public CustomVoterConfigurationContext license(License license) {
    this.license = license;
    return this;
  }

  public void commandLineEnv(TerracottaCommandLineEnvironment commandLineEnv) {
    this.commandLineEnv = commandLineEnv;
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
  public TerracottaCommandLineEnvironment commandLineEnv() {
    return commandLineEnv;
  }

  @Override
  public List<TerracottaVoter> getTerracottaVoters() {
    return terracottaVoters;
  }

  @Override
  public SecurityRootDirectory getSecurityRootDirectory() {
    return securityRootDirectory;
  }

  @Override
  public List<String> getHostNames() {
    List<String> hostNames = new ArrayList<>();
    for (TerracottaVoter terracottaVoter : terracottaVoters) {
      hostNames.add(terracottaVoter.getHostName());
    }
    return hostNames;
  }
}
