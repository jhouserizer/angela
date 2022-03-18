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
package org.terracotta.angela.common;

public class TerracottaConfigTool {
  private final String id;
  private final String hostName;

  private TerracottaConfigTool(String id, String hostName) {
    this.id = id;
    this.hostName = hostName;
  }

  public static TerracottaConfigTool configTool(String id, String hostName) {
    return new TerracottaConfigTool(id, hostName);
  }

  public String getId() {
    return id;
  }

  public String getHostName() {
    return hostName;
  }
}
