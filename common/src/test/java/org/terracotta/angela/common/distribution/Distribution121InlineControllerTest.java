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
package org.terracotta.angela.common.distribution;

import org.junit.Test;
import org.terracotta.angela.common.tcconfig.ServerSymbolicName;
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.angela.common.topology.PackageType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Distribution121InlineControllerTest {
  @Test
  public void testAddOptions() {
    Distribution distribution = mock(Distribution.class);
    when(distribution.getPackageType()).thenReturn(PackageType.KIT);
    Distribution121InlineController controller = new Distribution121InlineController(distribution);

    final TerracottaServer terracottaServer = mock(TerracottaServer.class);
    final ServerSymbolicName symbolicName = mock(ServerSymbolicName.class);
    when(terracottaServer.getServerSymbolicName()).thenReturn(symbolicName);
    when(terracottaServer.getHostName()).thenReturn("localhost");
    when(symbolicName.getSymbolicName()).thenReturn("Server1");
    when(terracottaServer.isReplica()).thenReturn(true);
    when(terracottaServer.getRelayHostName()).thenReturn("relay-hostname");
    when(terracottaServer.getRelayPort()).thenReturn(9410);
    when(terracottaServer.getRelayGroupPort()).thenReturn(9410);

    final File workingDir = new File("somedir");
    final List<String> options = controller.addOptions(terracottaServer, workingDir);

    assertThat(options.get(0), is(equalTo("-server-home")));
    assertThat(options.get(1), is(equalTo(new File("somedir").toString())));
    assertThat(options.get(2), is(equalTo("-name")));
    assertThat(options.get(3), is(equalTo("Server1")));
    assertThat(options.get(4), is(equalTo("-hostname")));
    assertThat(options.get(5), is(equalTo("localhost")));
    assertThat(options.get(6), is(equalTo("-replica")));
    assertThat(options.get(7), is(equalTo("true")));
    assertThat(options.get(8), is(equalTo("-relay-hostname")));
    assertThat(options.get(9), is(equalTo("relay-hostname")));
    assertThat(options.get(10), is(equalTo("-relay-port")));
    assertThat(options.get(11), is(equalTo("9410")));
    assertThat(options.get(12), is(equalTo("-relay-group-port")));
    assertThat(options.get(13), is(equalTo("9410")));
    assertThat(options.size(), is(14));
  }

  @Test
  public void testAddServerHomeWithOptions() {
    Distribution distribution = mock(Distribution.class);
    when(distribution.getPackageType()).thenReturn(PackageType.KIT);
    Distribution121InlineController controller = new Distribution121InlineController(distribution);

    List<String> options = new ArrayList<>();
    options.add("-name");
    options.add("Server2");
    options.add("-hostname");
    options.add("example.com");

    final File workingDir = new File("somedir");
    options = controller.addServerHome(options, workingDir);

    assertThat(options.get(0), is(equalTo("--server-home")));
    assertThat(options.get(1), is(equalTo(new File("somedir").toString())));
    assertThat(options.get(2), is(equalTo("-name")));
    assertThat(options.get(3), is(equalTo("Server2")));
    assertThat(options.get(4), is(equalTo("-hostname")));
    assertThat(options.get(5), is(equalTo("example.com")));
    assertThat(options.size(), is(6));
  }
}
