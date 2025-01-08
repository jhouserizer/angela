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
package org.terracotta.angela.client;

import org.junit.Test;
import org.terracotta.angela.client.config.TsaConfigurationContext;
import org.terracotta.angela.common.net.DefaultPortAllocator;
import org.terracotta.angela.common.tcconfig.License;
import org.terracotta.angela.common.tcconfig.TcConfig;
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.angela.common.topology.LicenseType;
import org.terracotta.angela.common.topology.PackageType;
import org.terracotta.angela.common.topology.Topology;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.terracotta.angela.common.distribution.Distribution.distribution;
import static org.terracotta.angela.common.topology.Version.version;

/**
 * @author Aurelien Broszniowski
 */

public class TsaTest {

  @Test
  public void testUrl10x() {
    TcConfig tcConfig = mock(TcConfig.class);
    License license = mock(License.class);
    TsaConfigurationContext tsaConfigurationContext = mock(TsaConfigurationContext.class);
    when(tsaConfigurationContext.getTopology()).then(invocationOnMock -> new Topology(distribution(version("3.8.1"), PackageType.KIT, LicenseType.TERRACOTTA_OS), tcConfig));
    when(tsaConfigurationContext.getLicense()).thenReturn(license);
    Tsa tsa = new Tsa(null, new DefaultPortAllocator(), null, tsaConfigurationContext);
    List<TerracottaServer> terracottaServerList = new ArrayList<>();
    terracottaServerList.add(TerracottaServer.server("1", "hostname1")
        .tsaPort(9510)
        .tsaGroupPort(9610)
        .managementPort(9810)
        .jmxPort(9910));
    terracottaServerList.add(TerracottaServer.server("2", "hostname2")
        .tsaPort(9511)
        .tsaGroupPort(9611)
        .managementPort(9811)
        .jmxPort(9911));
    when(tcConfig.getServers()).thenReturn(terracottaServerList);

    final URI uri = tsa.uri();
    assertThat(uri.toString(), is("terracotta://hostname1:9510,hostname2:9511"));

  }

  @Test
  public void testUrl4x() {
    TcConfig tcConfig = mock(TcConfig.class);
    License license = mock(License.class);
    TsaConfigurationContext tsaConfigurationContext = mock(TsaConfigurationContext.class);
    when(tsaConfigurationContext.getTopology()).then(invocationOnMock -> new Topology(distribution(version("4.3.6.0.0"), PackageType.KIT, LicenseType.GO), tcConfig));
    when(tsaConfigurationContext.getLicense()).thenReturn(license);
    Tsa tsa = new Tsa(null, new DefaultPortAllocator(), null, tsaConfigurationContext);
    List<TerracottaServer> terracottaServerList = new ArrayList<>();
    terracottaServerList.add(TerracottaServer.server("1", "hostname1")
        .tsaPort(9510)
        .tsaGroupPort(9610)
        .managementPort(9810)
        .jmxPort(9910));
    terracottaServerList.add(TerracottaServer.server("2", "hostname2")
        .tsaPort(9511)
        .tsaGroupPort(9611)
        .managementPort(9811)
        .jmxPort(9911));

    when(tcConfig.getServers()).thenReturn(terracottaServerList);

    final URI uri = tsa.uri();
    assertThat(uri.toString(), is("hostname1:9510,hostname2:9511"));

  }
}
