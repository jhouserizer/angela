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
import org.terracotta.angela.common.util.OS;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Distribution121ControllerTest {
  @Test
  public void testCreateSimpleTsaCommandForKit() {
    Distribution distribution = mock(Distribution.class);
    when(distribution.getPackageType()).thenReturn(PackageType.KIT);
    Distribution121Controller controller = new Distribution121Controller(distribution);

    final TerracottaServer terracottaServer = mock(TerracottaServer.class);
    final ServerSymbolicName symbolicName = mock(ServerSymbolicName.class);
    when(terracottaServer.getServerSymbolicName()).thenReturn(symbolicName);
    when(terracottaServer.getHostName()).thenReturn("localhost");
    when(symbolicName.getSymbolicName()).thenReturn("Server1");
    when(terracottaServer.isRelay()).thenReturn(true);
    when(terracottaServer.getReplicaHostName()).thenReturn("replica-hostname");
    when(terracottaServer.getReplicaPort()).thenReturn(9410);


    final File kitLocation = new File("/somedir");
    final List<String> args = new ArrayList<>();
    final List<String> tsaCommand = controller.createTsaCommand(terracottaServer, kitLocation, kitLocation, args);

    assertThat(tsaCommand.get(0), is(equalTo(new File("/somedir/server/bin/start-tc-server").getAbsolutePath() + OS.INSTANCE.getShellExtension())));
    assertThat(tsaCommand.get(1), is(equalTo("-name")));
    assertThat(tsaCommand.get(2), is(equalTo("Server1")));
    assertThat(tsaCommand.get(3), is(equalTo("-hostname")));
    assertThat(tsaCommand.get(4), is(equalTo("localhost")));
    assertThat(tsaCommand.get(5), is(equalTo("-relay")));
    assertThat(tsaCommand.get(6), is(equalTo("true")));
    assertThat(tsaCommand.get(7), is(equalTo("-replica-hostname")));
    assertThat(tsaCommand.get(8), is(equalTo("replica-hostname")));
    assertThat(tsaCommand.get(9), is(equalTo("-replica-port")));
    assertThat(tsaCommand.get(10), is(equalTo("9410")));
    assertThat(tsaCommand.size(), is(11));
  }

  @Test
  public void testCreateSimpleTsaCommandForKitWithArgs() {
    Distribution distribution = mock(Distribution.class);
    when(distribution.getPackageType()).thenReturn(PackageType.KIT);
    Distribution121Controller controller = new Distribution121Controller(distribution);

    final TerracottaServer terracottaServer = mock(TerracottaServer.class);
    final File kitLocation = new File("/somedir");

    final List<String> args = new ArrayList<>();
    args.add("-name");
    args.add("Server2");
    args.add("-hostname");
    args.add("example.com");

    final List<String> tsaCommand = controller.createTsaCommand(terracottaServer, kitLocation, kitLocation, args);

    assertThat(tsaCommand.get(0), is(equalTo(new File("/somedir/server/bin/start-tc-server").getAbsolutePath() + OS.INSTANCE.getShellExtension())));
    assertThat(tsaCommand.get(1), is(equalTo("-name")));
    assertThat(tsaCommand.get(2), is(equalTo("Server2")));
    assertThat(tsaCommand.get(3), is(equalTo("-hostname")));
    assertThat(tsaCommand.get(4), is(equalTo("example.com")));
    assertThat(tsaCommand.size(), is(5));
  }
}
