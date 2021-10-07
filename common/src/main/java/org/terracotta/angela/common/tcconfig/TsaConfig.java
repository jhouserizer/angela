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
package org.terracotta.angela.common.tcconfig;

import org.terracotta.angela.common.net.PortAllocator;
import org.terracotta.angela.common.topology.Version;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * @author Aurelien Broszniowski
 */

public class TsaConfig {

  private final List<TcConfig> tcConfigs;

  TsaConfig(List<TcConfig> tcConfigs) {
    this.tcConfigs = tcConfigs;
  }

  public void initialize(PortAllocator portAllocator) {
    tcConfigs.forEach(tcConfig -> tcConfig.initialize(portAllocator));
  }

  @SuppressWarnings("ConstantConditions")
  public static TsaConfig tsaConfig(Version version, List<TsaStripeConfig> stripeConfigs) {
    return tsaConfig(version, stripeConfigs, () -> new TcConfig(version, TsaConfig.class.getResource("tsa-config-tc-config-template-10.xml")));
  }

  public static TsaConfig tsaConfig(Version version, List<TsaStripeConfig> stripeConfigs, Supplier<TcConfig> tcConfigSupplier) {
    if (version.getMajor() < 10) {
      throw new UnsupportedOperationException("Dynamic TcConfig generation for BigMemory is not supported");
    }
    return new TsaConfig(buildTcConfigs(version, stripeConfigs, tcConfigSupplier));
  }

  public static TsaConfig tsaConfig(Version version, TsaStripeConfig stripeConfig, TsaStripeConfig... stripeConfigs) {
    List<TsaStripeConfig> cfgs = new ArrayList<>();
    cfgs.add(stripeConfig);
    cfgs.addAll(Arrays.asList(stripeConfigs));
    return tsaConfig(version, cfgs);
  }

  public static TsaConfig tsaConfig(TcConfig tcConfig, TcConfig... tcConfigs) {
    List<TcConfig> cfgs = new ArrayList<>();
    cfgs.add(tcConfig);
    cfgs.addAll(Arrays.asList(tcConfigs));
    return new TsaConfig(cfgs);
  }

  public static TsaConfig tsaConfig(List<TcConfig> tcConfigs) {
    return new TsaConfig(new ArrayList<>(tcConfigs));
  }

  public List<TcConfig> getTcConfigs() {
    return Collections.unmodifiableList(tcConfigs);
  }

  private static List<TcConfig> buildTcConfigs(Version version, List<TsaStripeConfig> stripeConfigs, Supplier<TcConfig> tcConfigSupplier) {
    List<TcConfig> tcConfigs = new ArrayList<>();

    for (int i = 0; i < stripeConfigs.size(); i++) {
      final TsaStripeConfig stripeConfig = stripeConfigs.get(i);
      TcConfig tcConfig = tcConfigSupplier.get();
      for (String hostname : stripeConfig.getHostnames()) {
        tcConfig.addServer((i + 1), hostname);
        tcConfig.setTcConfigName("tsa-config-" + hostname + "-stripe" + i + ".xml");
      }

      final TsaStripeConfig.TsaOffheapConfig tsaOffheapConfig = stripeConfig.getTsaOffheapConfig();
      if (tsaOffheapConfig != null) {
        tcConfig.addOffheap(tsaOffheapConfig.getResourceName(), tsaOffheapConfig.getSize(),
            tsaOffheapConfig.getUnit());
      }

      tcConfig.addDataDirectoryList(stripeConfig.getTsaDataDirectoryList());

      if (stripeConfig.getPersistenceDataName() != null) {
        tcConfig.addPersistencePlugin(stripeConfig.getPersistenceDataName());
      }

      tcConfigs.add(tcConfig);
    }

    return tcConfigs;
  }

  @Override
  public String toString() {
    return "TsaConfig{" +
        "tcConfigs=" + tcConfigs +
        '}';
  }
}
