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
package org.terracotta.angela.client.filesystem;

import org.apache.ignite.Ignite;
import org.terracotta.angela.agent.Agent;
import org.terracotta.angela.client.util.IgniteClientHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class RemoteFile {
  protected final Ignite ignite;
  protected final String hostname;
  private final int ignitePort;
  protected final String parentName;
  protected final String name;

  public RemoteFile(Ignite ignite, String hostname, int ignitePort, String parentName, String name) {
    this.ignite = ignite;
    this.hostname = hostname;
    this.ignitePort = ignitePort;
    this.parentName = parentName;
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public String getAbsoluteName() {
    if (parentName == null) {
      return name;
    }
    return parentName + "/" + name;
  }

  public boolean isFolder() {
    return this instanceof RemoteFolder;
  }

  public void downloadTo(File path) throws IOException {
    byte[] bytes = downloadContents();
    try (FileOutputStream fos = new FileOutputStream(path)) {
      fos.write(bytes);
    }
  }

  private byte[] downloadContents() {
    String filename = getAbsoluteName();
    return IgniteClientHelper.executeRemotely(ignite, hostname, ignitePort, () -> Agent.controller.downloadFile(filename));
  }

  public TransportableFile toTransportableFile() {
    return new TransportableFile(getName(), downloadContents());
  }

  @Override
  public String toString() {
    return "[" + hostname + "]:" + name;
  }
}
