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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;

public class IpUtils {

  private static final String LOCAL_HOSTNAME;

  // caches costly resolutions
  static {
    try {
      LOCAL_HOSTNAME = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Try to determine (but not accurately) if a given name matches a local address.
   * A local address is a local IP or local hostname.
   * <p>
   * This "name" can be any Angela generated names (i.e. in case of client arrays instanceId or symbolic names),
   * we do not use "getByName()" to avoid any dns resolution
   */
  public static boolean isLocal(String name) {
    switch (name) {
      case "localhost":
      case "127.0.0.1":
      case " ::1":
        return true;
      default:
        return LOCAL_HOSTNAME.equals(name) || name.startsWith("169.254.") || name.startsWith("fe80:");
    }
  }

  public static boolean areAllLocal(Collection<String> targetServerNames) {
    for (String targetServerName : targetServerNames) {
      if (!isLocal(targetServerName)) {
        return false;
      }
    }
    return true;
  }

  public static String getHostName() {
    return LOCAL_HOSTNAME;
  }
}
