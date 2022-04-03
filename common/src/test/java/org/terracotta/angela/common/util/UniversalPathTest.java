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

import org.junit.Test;

import java.nio.file.FileSystems;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

/**
 * @author Mathieu Carbou
 */
public class UniversalPathTest {
  @Test
  public void fromLocalPath() {
    assertEquals("a", UniversalPath.fromLocalPath(Paths.get("a")).toString());
    assertEquals("a", UniversalPath.fromLocalPath(Paths.get("a")).toLocalPath().toString());

    assertEquals("a/a", UniversalPath.fromLocalPath(Paths.get("a/a")).toString());
    assertEquals("/", UniversalPath.fromLocalPath(Paths.get("/")).toString());
    assertEquals("/a/a", UniversalPath.fromLocalPath(Paths.get("/a/a")).toString());

    assertEquals("a", UniversalPath.fromLocalPath(Paths.get("a")).toLocalPath().toString());

    if (OS.INSTANCE.isWindows()) {
      String root = FileSystems.getDefault().getRootDirectories().iterator().next().toString();
      assertEquals("a/a", UniversalPath.fromLocalPath(Paths.get("a\\a")).toString());
      assertEquals("a\\a", UniversalPath.fromLocalPath(Paths.get("a\\a")).toLocalPath().toString());

      assertEquals("/", UniversalPath.fromLocalPath(Paths.get("c:\\")).toString());
      assertEquals(root, UniversalPath.fromLocalPath(Paths.get("c:\\")).toLocalPath().toString());

      assertEquals("/a/a", UniversalPath.fromLocalPath(Paths.get("c:\\a\\a")).toString());
      assertEquals(root + "a\\a", UniversalPath.fromLocalPath(Paths.get("c:\\a\\a")).toLocalPath().toString());

    } else {
      assertEquals("a/a", UniversalPath.fromLocalPath(Paths.get("a/a")).toLocalPath().toString());
      assertEquals("/", UniversalPath.fromLocalPath(Paths.get("/")).toLocalPath().toString());
      assertEquals("/a/a", UniversalPath.fromLocalPath(Paths.get("/a/a")).toLocalPath().toString());
    }
  }
}