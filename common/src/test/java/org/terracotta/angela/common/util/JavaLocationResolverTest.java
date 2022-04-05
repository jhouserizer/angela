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

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;

public class JavaLocationResolverTest {

  @Test(expected = NullPointerException.class)
  public void testNullResourceThrows() {
    new JavaLocationResolver(null);
  }

  @Test
  public void testResolveAll() throws Exception {
    try (InputStream inputStream = getClass().getResourceAsStream("/toolchains/toolchains.xml")) {
      JavaLocationResolver javaLocationResolver = new JavaLocationResolver(inputStream);
      List<JDK> jdks = javaLocationResolver.resolveJavaLocations(null, emptySet(), true);
      assertThat(jdks.size(), is(12));
      assertThat(jdks.get(0).getVendor().get(), is(""));
      assertThat(jdks.get(0).getVersion().get(), is("1.6"));
      assertFalse(jdks.get(1).getVendor().isPresent());
      assertThat(jdks.get(1).getVersion().get(), is("1.7"));
      assertThat(jdks.get(2).getVendor().get(), is("openjdk"));
      assertThat(jdks.get(2).getVersion().get(), is("1.8"));
      assertThat(jdks.get(3).getVendor().get(), is("ibm"));
      assertFalse(jdks.get(3).getVersion().isPresent());
      assertThat(jdks.get(4).getVendor().get(), is(""));
      assertThat(jdks.get(4).getVersion().get(), is("1.8"));
      assertFalse(jdks.get(5).getVendor().isPresent());
      assertFalse(jdks.get(5).getVersion().isPresent());
      assertThat(jdks.get(6).getVendor().get(), is("zulu"));
      assertThat(jdks.get(6).getVersion().get(), is("1.8"));
    }
  }

  @Test
  public void testResolveOne() throws Exception {
    try (InputStream inputStream = getClass().getResourceAsStream("/toolchains/toolchains.xml")) {
      JavaLocationResolver javaLocationResolver = new JavaLocationResolver(inputStream);
      List<JDK> jdks = javaLocationResolver.resolveJavaLocations("1.8", singleton("zulu"), true);
      assertThat(jdks.size(), is(1));
      assertThat(jdks.get(0).getVendor().get(), is("zulu"));
      assertThat(jdks.get(0).getVersion().get(), is("1.8"));
    }
  }

  @Test
  public void testResolveManyOfaVendor() throws Exception {
    try (InputStream inputStream = getClass().getResourceAsStream("/toolchains/toolchains.xml")) {
      JavaLocationResolver javaLocationResolver = new JavaLocationResolver(inputStream);
      List<JDK> jdks = javaLocationResolver.resolveJavaLocations(null, new HashSet<>(singletonList("zulu")), true);
      assertThat(jdks.size(), is(2));
      assertThat(jdks.get(0).getVendor().get(), is("zulu"));
      assertThat(jdks.get(0).getVersion().get(), is("1.8"));
      assertThat(jdks.get(1).getVendor().get(), is("zulu"));
      assertThat(jdks.get(1).getVersion().get(), is("11"));
    }
  }

  @Test
  public void testResolveManyOfaVersion() throws Exception {
    try (InputStream inputStream = getClass().getResourceAsStream("/toolchains/toolchains.xml")) {
      JavaLocationResolver javaLocationResolver = new JavaLocationResolver(inputStream);
      List<JDK> jdks = javaLocationResolver.resolveJavaLocations("11", new HashSet<>(asList("sun", "zulu")), true);
      assertThat(jdks.size(), is(2));
      assertThat(jdks.get(0).getVendor().get(), is("zulu"));
      assertThat(jdks.get(0).getVersion().get(), is("11"));
      assertThat(jdks.get(1).getVendor().get(), is("sun"));
      assertThat(jdks.get(1).getVersion().get(), is("11"));
    }
  }

  @Test
  public void testResolveAllOfaVersion() throws Exception {
    try (InputStream inputStream = getClass().getResourceAsStream("/toolchains/toolchains.xml")) {
      JavaLocationResolver javaLocationResolver = new JavaLocationResolver(inputStream);
      List<JDK> jdks = javaLocationResolver.resolveJavaLocations("11", emptySet(), true);
      assertThat(jdks.size(), is(3));
      assertThat(jdks.get(0).getVendor().get(), is("zulu"));
      assertThat(jdks.get(0).getVersion().get(), is("11"));
      assertThat(jdks.get(1).getVendor().get(), is("sun"));
      assertThat(jdks.get(1).getVersion().get(), is("11"));
      assertThat(jdks.get(2).getVendor().get(), is("foo"));
      assertThat(jdks.get(2).getVersion().get(), is("11"));
    }
  }
}
