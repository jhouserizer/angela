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

import org.hamcrest.core.IsNull;
import org.junit.Test;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class JavaLocationResolverTest {

  @Test(expected = NullPointerException.class)
  public void testNullResourceThrows() {
    new JavaLocationResolver(null);
  }

  @Test
  public void testResolveAll() throws Exception {
    try (InputStream inputStream = getClass().getResourceAsStream("/toolchains/toolchains.xml")) {
      JavaLocationResolver javaLocationResolver = new JavaLocationResolver(inputStream);
      List<JDK> jdks = javaLocationResolver.resolveJavaLocations("", emptySet(), true);
      assertThat(jdks.size(), is(7));
      assertThat(jdks.get(0).getVendor(), is("sun"));
      assertThat(jdks.get(0).getVersion(), is("1.6"));
      assertThat(jdks.get(1).getVendor(), is(IsNull.nullValue()));
      assertThat(jdks.get(1).getVersion(), is("1.7"));
      assertThat(jdks.get(2).getVendor(), is("openjdk"));
      assertThat(jdks.get(2).getVersion(), is("1.8"));
      assertThat(jdks.get(3).getVendor(), is("ibm"));
      assertThat(jdks.get(3).getVersion(), is("1.8"));
      assertThat(jdks.get(4).getVendor(), is("sun"));
      assertThat(jdks.get(4).getVersion(), is("1.8"));
      assertThat(jdks.get(5).getVendor(), is("Oracle Corporation"));
      assertThat(jdks.get(5).getVersion(), is("1.8"));
      assertThat(jdks.get(6).getVendor(), is("zulu"));
      assertThat(jdks.get(6).getVersion(), is("1.8"));
    }
  }

  @Test
  public void testResolveOne() throws Exception {
    try (InputStream inputStream = getClass().getResourceAsStream("/toolchains/toolchains.xml")) {
      JavaLocationResolver javaLocationResolver = new JavaLocationResolver(inputStream);
      List<JDK> jdks = javaLocationResolver.resolveJavaLocations("1.6", singleton("sun"), true);
      assertThat(jdks.size(), is(1));
      assertThat(jdks.get(0).getVendor(), is("sun"));
      assertThat(jdks.get(0).getVersion(), is("1.6"));
    }
  }

  @Test
  public void testResolveManyOfaVendor() throws Exception {
    try (InputStream inputStream = getClass().getResourceAsStream("/toolchains/toolchains.xml")) {
      JavaLocationResolver javaLocationResolver = new JavaLocationResolver(inputStream);
      List<JDK> jdks = javaLocationResolver.resolveJavaLocations("", new HashSet<>(singletonList("sun")), true);
      assertThat(jdks.size(), is(2));
      assertThat(jdks.get(0).getVendor(), is("sun"));
      assertThat(jdks.get(0).getVersion(), is("1.6"));
      assertThat(jdks.get(1).getVendor(), is("sun"));
      assertThat(jdks.get(1).getVersion(), is("1.8"));
    }
  }

  @Test
  public void testResolveManyOfaVersion() throws Exception {
    try (InputStream inputStream = getClass().getResourceAsStream("/toolchains/toolchains.xml")) {
      JavaLocationResolver javaLocationResolver = new JavaLocationResolver(inputStream);
      List<JDK> jdks = javaLocationResolver.resolveJavaLocations("1.8", new HashSet<>(asList("Oracle Corporation", "openjdk")), true);
      assertThat(jdks.size(), is(2));
      assertThat(jdks.get(0).getVendor(), is("openjdk"));
      assertThat(jdks.get(0).getVersion(), is("1.8"));
      assertThat(jdks.get(1).getVendor(), is("Oracle Corporation"));
      assertThat(jdks.get(1).getVersion(), is("1.8"));
    }
  }

  @Test
  public void testResolveAllOfaVersion() throws Exception {
    try (InputStream inputStream = getClass().getResourceAsStream("/toolchains/toolchains.xml")) {
      JavaLocationResolver javaLocationResolver = new JavaLocationResolver(inputStream);
      List<JDK> jdks = javaLocationResolver.resolveJavaLocations("1.8", new HashSet<>(asList("Oracle Corporation", "sun", "openjdk")), true);
      assertThat(jdks.size(), is(3));
      assertThat(jdks.get(0).getVendor(), is("openjdk"));
      assertThat(jdks.get(0).getVersion(), is("1.8"));
      assertThat(jdks.get(1).getVendor(), is("sun"));
      assertThat(jdks.get(1).getVersion(), is("1.8"));
      assertThat(jdks.get(2).getVendor(), is("Oracle Corporation"));
      assertThat(jdks.get(2).getVersion(), is("1.8"));
    }
  }

  @Test
  public void testResolveAllOfaVersionNoVendorsSpecified() throws Exception {
    try (InputStream inputStream = getClass().getResourceAsStream("/toolchains/toolchains.xml")) {
      JavaLocationResolver javaLocationResolver = new JavaLocationResolver(inputStream);
      List<JDK> jdks = javaLocationResolver.resolveJavaLocations("1.8", emptySet(), true);
      assertThat(jdks.size(), is(5));
    }
  }
}
