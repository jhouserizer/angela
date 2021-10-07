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
package org.terracotta.angela.common.tcconfig;

import org.terracotta.angela.common.net.PortAllocator;
import org.terracotta.angela.common.topology.Version;

import java.net.URL;

public class EnterpriseTcConfig extends TcConfig {

  public static EnterpriseTcConfig eeTcConfig(Version version, URL tcConfigPath) {
    return new EnterpriseTcConfig(version, tcConfigPath);
  }

  EnterpriseTcConfig(EnterpriseTcConfig tcConfig) {
    super(tcConfig);
  }

  EnterpriseTcConfig(Version version, URL tcConfigPath) {
    super(version, tcConfigPath);
  }

  @Override
  public EnterpriseTcConfig copy() {
    return new EnterpriseTcConfig(this);
  }

  public void initialize(PortAllocator portAllocator) {
    tcConfigHolder.initialize(portAllocator, tag -> true);
  }

}
