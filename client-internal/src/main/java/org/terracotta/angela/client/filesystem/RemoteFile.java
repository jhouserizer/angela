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
package org.terracotta.angela.client.filesystem;

import org.terracotta.angela.agent.AgentController;
import org.terracotta.angela.agent.com.AgentExecutor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RemoteFile {
  protected final transient AgentExecutor agentExecutor;
  protected final String parentName;
  protected final String name;

  public RemoteFile(AgentExecutor agentExecutor, String parentName, String name) {
    this.agentExecutor = agentExecutor;
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
    downloadTo(path.toPath());
  }

  public void downloadTo(Path path) throws IOException {
    Files.write(path, downloadContents());
  }

  private byte[] downloadContents() {
    String filename = getAbsoluteName();
    return agentExecutor.execute(() -> AgentController.getInstance().downloadFile(filename));
  }

  public TransportableFile toTransportableFile() {
    return new TransportableFile(getName(), downloadContents());
  }

  @Override
  public String toString() {
    return "[" + agentExecutor.getTarget() + "]:" + name;
  }
}
