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
  public static boolean isLocal(String targetServerName) {
    InetAddress address;
    try {
      address = InetAddress.getByName(targetServerName);
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
    return address.isLoopbackAddress() || address.isLinkLocalAddress();
  }

  public static boolean areAllLocal(Collection<String> targetServerNames) {
    for (String targetServerName : targetServerNames) {
      if (!isLocal(targetServerName)) {
        return false;
      }
    }
    return true;
  }

  public static boolean isAnyLocal(Collection<String> targetServerNames) {
    for (String targetServerName : targetServerNames) {
      if (isLocal(targetServerName)) {
        return true;
      }
    }
    return false;
  }

  public static String getHostName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getHostAddress(String host) {
    try {
      return InetAddress.getByName(host).getHostAddress();
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }
}
