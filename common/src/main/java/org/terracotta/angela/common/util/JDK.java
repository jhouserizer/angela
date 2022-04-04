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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;

public class JDK {

  private final String home;
  private final String version;
  private final String vendor;

  // represent an entry in toolchain file.
  // the entry might be incorrectly defined
  JDK(String home, String version, String vendor) {
    this.home = home;
    this.version = version; // null allowed
    this.vendor = vendor; // null allowed

    if (home.trim().isEmpty()) {
      throw new IllegalArgumentException(home);
    }
  }

  public String getHome() {
    return home;
  }

  public Optional<String> getVersion() {
    return Optional.ofNullable(version);
  }

  public Optional<String> getVendor() {
    return Optional.ofNullable(vendor);
  }

  @Override
  public String toString() {
    return "JDK{" +
        "home='" + home + '\'' +
        ", version='" + version + '\'' +
        ", vendor='" + vendor + '\'' +
        '}';
  }

  public boolean canBeLocated() {
    return Files.isDirectory(Paths.get(getHome()));
  }

  public boolean matches(String allowedVersion, Set<String> allowedVendors) {
    return (allowedVersion == null || allowedVersion.equalsIgnoreCase(this.version))
        && (allowedVendors.isEmpty() || allowedVendors.stream().anyMatch(allowed -> allowed.equalsIgnoreCase(this.vendor)));
  }
}
