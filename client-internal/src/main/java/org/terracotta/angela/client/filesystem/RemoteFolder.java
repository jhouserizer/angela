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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.IOUtils;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteException;
import org.apache.ignite.lang.IgniteCallable;
import org.terracotta.angela.agent.Agent;
import org.terracotta.angela.client.util.IgniteClientHelper;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.util.stream.Collectors.toList;

public class RemoteFolder extends RemoteFile {
  private final int ignitePort;

  public RemoteFolder(Ignite ignite, String hostname, int ignitePort, String parentName, String name) {
    super(ignite, hostname, ignitePort, parentName, name);
    this.ignitePort = ignitePort;
  }

  public List<RemoteFile> list() {
    String absoluteName = getAbsoluteName();
    IgniteCallable<List<String>> filesCallable = () -> Agent.controller.listFiles(absoluteName);
    List<String> remoteFiles = IgniteClientHelper.executeRemotely(ignite, hostname, ignitePort, filesCallable);

    IgniteCallable<List<String>> foldersCallable = () -> Agent.controller.listFolders(absoluteName);
    List<String> remoteFolders = IgniteClientHelper.executeRemotely(ignite, hostname, ignitePort, foldersCallable);

    List<RemoteFile> result = new ArrayList<>();
    result.addAll(remoteFiles.stream()
        .map(s -> new RemoteFile(ignite, hostname, ignitePort, getAbsoluteName(), s))
        .collect(toList()));
    result.addAll(remoteFolders.stream()
        .map(s -> new RemoteFolder(ignite, hostname, ignitePort, getAbsoluteName(), s))
        .collect(toList()));
    return result;
  }

  public void upload(File localFile) throws IOException {
    if (localFile.isDirectory()) {
      uploadFolder(".", localFile);
    } else {
      try (FileInputStream fis = new FileInputStream(localFile)) {
        upload(localFile.getName(), fis);
      }
    }
  }

  @SuppressWarnings("ConstantConditions")
  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  private void uploadFolder(String parentName, File folder) throws IOException {
    File[] files = folder.listFiles();
    for (File f : files) {
      String currentName = parentName + "/" + f.getName();
      if (f.isDirectory()) {
        uploadFolder(currentName, f);
      } else {
        try (FileInputStream fis = new FileInputStream(f)) {
          upload(currentName, fis);
        }
      }
    }
  }

  public void upload(String remoteFilename, URL localResourceUrl) throws IOException {
    try (InputStream in = localResourceUrl.openStream()) {
      upload(remoteFilename, in);
    }
  }

  public void upload(String remoteFilename, InputStream localStream) throws IOException {
    byte[] data = IOUtils.toByteArray(localStream);
    String filename = getAbsoluteName() + "/" + remoteFilename;
    IgniteClientHelper.executeRemotely(ignite, hostname, ignitePort, () -> Agent.controller.uploadFile(filename, data));
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
  @Override
  public void downloadTo(File localPath) throws IOException {
    String foldername = getAbsoluteName();
    byte[] bytes;
    try {
      bytes = IgniteClientHelper.executeRemotely(ignite, hostname, ignitePort, () -> Agent.controller.downloadFolder(foldername));
    } catch (IgniteException ie) {
      throw new IOException("Error downloading remote folder '" + foldername + "' into local folder '" + localPath + "'", ie);
    }

    localPath.mkdirs();
    if (!localPath.isDirectory()) {
      throw new IllegalArgumentException("Destination path '" + localPath + "' is not a folder or could not be created");
    }

    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
      while (true) {
        ZipEntry nextEntry = zis.getNextEntry();
        if (nextEntry == null) {
          break;
        }
        String name = nextEntry.getName();
        File file = new File(localPath, name);
        file.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
          IOUtils.copy(zis, fos);
        }
      }
    }
  }

  @Override
  public String toString() {
    return super.toString() + "/";
  }
}
