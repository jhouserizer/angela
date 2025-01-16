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
package org.terracotta.angela.common.clientconfig;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Aurelien Broszniowski
 */

public class ClientId implements Serializable {
  private static final long serialVersionUID = 1L;

  private final ClientSymbolicName symbolicName;
  private final String hostname;

  public ClientId(ClientSymbolicName symbolicName, String hostname) {
    this.symbolicName = Objects.requireNonNull(symbolicName);
    this.hostname = Objects.requireNonNull(hostname);
  }

  public ClientSymbolicName getSymbolicName() {
    return symbolicName;
  }

  /**
   * @deprecated Use {@link #getHostName()} instead
   */
  @Deprecated
  public String getHostname() {
    return getHostName();
  }

  public String getHostName() {
    return hostname;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ClientId clientId = (ClientId) o;
    return Objects.equals(symbolicName, clientId.symbolicName) &&
        Objects.equals(hostname, clientId.hostname);
  }

  @Override
  public int hashCode() {
    return Objects.hash(symbolicName, hostname);
  }

  @Override
  public String toString() {
    return symbolicName + "@" + hostname;
  }
}
