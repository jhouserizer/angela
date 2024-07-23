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
package org.terracotta.angela.common.util;

import java.io.Serializable;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A raw path is a {@link Path} that is keeping the user input as-is
 * and only parses it when used.
 * <p>
 * Dynamic Config needs to keep all user inputs.
 * <p>
 * The problems with Path are:
 * <p>
 * 1. Parsing: Paths.get() will parse some user input into some segments.
 * The input and parsing is per-platform so it can lead to
 * different outcomes in both parsing and toString()
 * <p>
 * - On Win: Paths.get("a/b") and Paths.get("a\\b") give 2 segments
 * <p>
 * - On Lin: Paths.get("a/b") give 2 segments and Paths.get("a\\b") gives 1 segment
 * <p>
 * 2. User input is lost: when parsed, we loose the path separator that was initially used.
 * As a consequence, all serializations could lead to different results that is different
 * than the user input since it depends on the system where we are calling toString().
 * <p>
 * So this class aims at resolving all of that, by keeping the user input,
 * that will be used for equality checks, serialization and deserialization,
 * and also provide the equivalent of the Path API when used.
 * <p>
 * The parsing of the user input will be only done when the object will be queried and
 * converted to a Path
 *
 * @author Mathieu Carbou
 */
public class UniversalPath implements Serializable {
  private static final long serialVersionUID = 1L;

  private final boolean absolute;
  private final List<String> segments;
  private volatile transient Path cache;

  public static UniversalPath fromLocalPath(Path local) {
    Path root = local.getRoot();
    if (root != null) {
      local = root.relativize(local);
    }
    List<String> segments = new ArrayList<>(local.getNameCount());
    for (Path segment : local) {
      segments.add(segment.toString());
    }
    return new UniversalPath(root != null, segments);
  }

  public static UniversalPath create(boolean absolute, List<String> segments) {
    return new UniversalPath(absolute, segments);
  }

  public static UniversalPath create(boolean absolute, String... segments) {
    return new UniversalPath(absolute, Arrays.asList(segments));
  }

  private UniversalPath(boolean absolute, List<String> segments) {
    this.absolute = absolute;
    for (String segment : segments) {
      if (segment.contains("/")) {
        throw new IllegalArgumentException("Incorrect path: " + segments);
      }
    }
    this.segments = new ArrayList<>(segments);
  }

  public Path toLocalPath() {
    if (cache != null) {
      return cache;
    }
    // note: this is completely OK if several threads are creating the same value.
    // Method does not need synchronization.
    Path rel = Paths.get("", this.segments.toArray(new String[0]));
    // if absolute, we assume first file system found since we have no way to know when transferring
    // from linux to win what the default FS should be for Windows.
    return cache = absolute ? FileSystems.getDefault().getRootDirectories().iterator().next().resolve(rel) : rel;
  }

  @Override
  public String toString() {
    return (absolute ? "/" : "") + String.join("/", segments);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof UniversalPath)) return false;
    UniversalPath that = (UniversalPath) o;
    return absolute == that.absolute && segments.equals(that.segments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(absolute, segments);
  }
}
