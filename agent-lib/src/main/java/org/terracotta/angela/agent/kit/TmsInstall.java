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
package org.terracotta.angela.agent.kit;

import org.terracotta.angela.common.TerracottaCommandLineEnvironment;
import org.terracotta.angela.common.TerracottaManagementServerInstance;
import org.terracotta.angela.common.distribution.Distribution;

import java.io.File;


/**
 * Installation instance of a TerracottaManagementServer
 */
public class TmsInstall {

  private final Distribution distribution;
  private final File kitLocation;
  private final File workingDir;
  private final TerracottaCommandLineEnvironment tcEnv;
  private TerracottaManagementServerInstance terracottaManagementServerInstance;

  public File getKitLocation() {
    return kitLocation;
  }

  public File getWorkingDir() {
    return workingDir;
  }

  public TmsInstall(Distribution distribution, File kitLocation, File workingDir, TerracottaCommandLineEnvironment tcEnv) {
    this.distribution = distribution;
    this.kitLocation = kitLocation;
    this.workingDir = workingDir;
    this.tcEnv = tcEnv;
    addTerracottaManagementServer();
  }

  public void addTerracottaManagementServer() {
    terracottaManagementServerInstance = new TerracottaManagementServerInstance(distribution.createDistributionController(), kitLocation, workingDir, tcEnv);
  }

  public TerracottaManagementServerInstance getTerracottaManagementServerInstance() {
    return terracottaManagementServerInstance;
  }

  public void removeServer() {
    terracottaManagementServerInstance = null;
  }

}
