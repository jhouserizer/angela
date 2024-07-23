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
package org.terracotta.angela.client.config.custom;

import org.terracotta.angela.client.config.ClientArrayConfigurationContext;
import org.terracotta.angela.common.TerracottaCommandLineEnvironment;
import org.terracotta.angela.common.tcconfig.License;
import org.terracotta.angela.common.topology.ClientArrayTopology;

public class CustomClientArrayConfigurationContext implements ClientArrayConfigurationContext {
  private ClientArrayTopology clientArrayTopology;
  private License license;
  private TerracottaCommandLineEnvironment terracottaCommandLineEnvironment = TerracottaCommandLineEnvironment.DEFAULT;

  protected CustomClientArrayConfigurationContext() {
  }

  @Override
  public ClientArrayTopology getClientArrayTopology() {
    return clientArrayTopology;
  }

  public CustomClientArrayConfigurationContext clientArrayTopology(ClientArrayTopology clientArrayTopology) {
    this.clientArrayTopology = clientArrayTopology;
    return this;
  }

  @Override
  public License getLicense() {
    return license;
  }

  public CustomClientArrayConfigurationContext license(License license) {
    this.license = license;
    return this;
  }

  @Override
  public TerracottaCommandLineEnvironment getTerracottaCommandLineEnvironment() {
    return terracottaCommandLineEnvironment;
  }

  public CustomClientArrayConfigurationContext terracottaCommandLineEnvironment(TerracottaCommandLineEnvironment terracottaCommandLineEnvironment) {
    this.terracottaCommandLineEnvironment = terracottaCommandLineEnvironment;
    return this;
  }
}
