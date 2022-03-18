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
import java.util.HashMap;
import java.util.Map;

public class SecureTcConfig extends EnterpriseTcConfig {
  private static final long serialVersionUID = 1L;

  private final Map<ServerSymbolicName, SecurityRootDirectory> securityRootDirectoryMap = new HashMap<>();
  private final boolean validateConfig;

  public static SecureTcConfig secureTcConfig(Version version,
                                              URL tcConfigPath,
                                              NamedSecurityRootDirectory... namedSecurityRootDirectories) {
    return new SecureTcConfig(version, tcConfigPath, true, namedSecurityRootDirectories);
  }

  public static SecureTcConfig secureTcConfig(Version version,
                                              URL tcConfigPath,
                                              boolean validateConfig,
                                              NamedSecurityRootDirectory... namedSecurityRootDirectories) {
    return new SecureTcConfig(version, tcConfigPath, validateConfig, namedSecurityRootDirectories);
  }

  SecureTcConfig(Version version, URL tcConfigPath, boolean validateConfig, NamedSecurityRootDirectory... namedSecurityRootDirectories) {
    super(version, tcConfigPath);
    this.validateConfig = validateConfig;
    for (NamedSecurityRootDirectory namedSecurityRootDirectory : namedSecurityRootDirectories) {
      securityRootDirectoryMap.put(namedSecurityRootDirectory.getServerSymbolicName(),
          namedSecurityRootDirectory.getSecurityRootDirectory());
    }
  }

  SecureTcConfig(SecureTcConfig tcConfig, Map<ServerSymbolicName, SecurityRootDirectory> securityRootDirectoryMap) {
    super(tcConfig);
    this.securityRootDirectoryMap.putAll(securityRootDirectoryMap);
    this.validateConfig = tcConfig.validateConfig;
  }

  @Override
  public void initialize(PortAllocator portAllocator) {
    super.initialize(portAllocator);
    if (validateConfig) {
      validateConfig();
    }
  }

  @Override
  public SecureTcConfig copy() {
    return new SecureTcConfig(this, securityRootDirectoryMap);
  }

  private void validateConfig() {
    getServers().forEach(terracottaServer -> {
      ServerSymbolicName serverSymbolicName = terracottaServer.getServerSymbolicName();
      if (!securityRootDirectoryMap.containsKey(serverSymbolicName)) {
        throw new IllegalArgumentException("NamedSecurityRootDirectory is not provided for server " +
            serverSymbolicName.getSymbolicName());
      }
    });

    if (securityRootDirectoryMap.size() != getServers().size()) {
      throw new IllegalArgumentException("Given NamedSecurityRootDirectory(s) contains extra servers " +
          "which are not present in tc-config, perhaps some server configurations " +
          "are missing from tc-config?");
    }
  }

  public SecurityRootDirectory securityRootDirectoryFor(ServerSymbolicName serverSymbolicName) {
    return securityRootDirectoryMap.get(serverSymbolicName);
  }

}
