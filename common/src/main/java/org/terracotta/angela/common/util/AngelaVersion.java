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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AngelaVersion {

  private static final String ANGELA_VERSION;

  static {
    try {
      Properties versionsProps = new Properties();
      try (InputStream in = AngelaVersion.class.getResourceAsStream("/angela/angela-version.properties")) {
        versionsProps.load(in);
      }
      ANGELA_VERSION = versionsProps.getProperty("angela.version");
    } catch (IOException ioe) {
      throw new RuntimeException("Cannot find /angela/angela-version.properties in classpath");
    }
  }

  public static String getAngelaVersion() {
    return ANGELA_VERSION;
  }
}
