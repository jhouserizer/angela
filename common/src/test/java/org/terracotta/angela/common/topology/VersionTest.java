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
package org.terracotta.angela.common.topology;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Aurelien Broszniowski
 */

public class VersionTest {

  @Test
  public void test5digitsVersion() {
    final Version version = new Version("1.7.0.2.124");
    assertThat(version.getMajor(), equalTo(1));
    assertThat(version.getMinor(), equalTo(7));
    assertThat(version.getRevision(), equalTo(0));
    assertThat(version.getBuild_major(), equalTo(2));
    assertThat(version.getBuild_minor(), equalTo(124));
    assertThat(version.getShortVersion(), equalTo("1.7.0"));
  }

  @Test
  public void testpreRelease() {
    final Version version = new Version("1.5.0-pre11");
    assertThat(version.getMajor(), equalTo(1));
    assertThat(version.getMinor(), equalTo(5));
    assertThat(version.getRevision(), equalTo(0));
    assertThat(version.getShortVersion(), equalTo("1.5.0"));
    assertThat(version.isSnapshot(), equalTo(false));

  }

}
