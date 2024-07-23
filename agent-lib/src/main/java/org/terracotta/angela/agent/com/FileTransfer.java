/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.angela.agent.com;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FileTransfer implements Serializable {
  private static final long serialVersionUID = 1L;

  public static final FileTransfer END = new FileTransfer(null, null);

  private final String relativePath; // unix-like
  private final byte[] bytes;

  private FileTransfer(String relativePath, byte[] bytes) {
    this.relativePath = relativePath;
    this.bytes = bytes;
  }

  public boolean isFinished() {
    return relativePath == null && bytes == null;
  }

  @Override
  public String toString() {
    return relativePath;
  }

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  public void writeTo(Path root) {
    try {
      Path dest = root.resolve(relativePath);
      Files.createDirectories(dest.getParent());
      Files.write(dest, bytes);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  public static FileTransfer from(Path root, Path file) {
    try {
      if (Files.isRegularFile(root)) {
        // if root is a file, then walk will have 1 entry where root == file
        if (!Objects.equals(root, file)) {
          throw new IllegalStateException(root + " vs " + file);
        }
        return new FileTransfer(root.getFileName().toString(), Files.readAllBytes(file));

      } else {
        // relative path which contains the root folder name as a base
        Path relWithBase = root.getFileName().resolve(root.relativize(file));
        List<String> parts = new ArrayList<>(relWithBase.getNameCount());
        relWithBase.forEach(part -> parts.add(part.toString()));
        return new FileTransfer(String.join("/", parts), Files.readAllBytes(file));
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
