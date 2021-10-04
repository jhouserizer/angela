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
package org.terracotta.angela.client.config.custom;

import org.terracotta.angela.client.config.ClientArrayConfigurationContext;
import org.terracotta.angela.client.config.TmsConfigurationContext;
import org.terracotta.angela.client.config.TsaConfigurationContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class CustomMultiConfigurationContext extends CustomConfigurationContext {

  public static CustomMultiConfigurationContext customMultiConfigurationContext() {
    return new CustomMultiConfigurationContext();
  }

  private CustomMultiConfigurationContext() {
  }

  private final List<CustomTsaConfigurationContext> customTsaConfigurationContexts = new ArrayList<>();
  private int customTsaConfigurationContextsIndex = 0;
  private final List<CustomTmsConfigurationContext> customTmsConfigurationContexts = new ArrayList<>();
  private int customTmsConfigurationContextsIndex = 0;
  private final List<CustomClientArrayConfigurationContext> customClientArrayConfigurationContexts = new ArrayList<>();
  private int customClientArrayConfigurationContextsIndex = 0;

  @Override
  public TsaConfigurationContext tsa() {
    if (customTsaConfigurationContextsIndex >= customTsaConfigurationContexts.size()) {
      throw new IllegalStateException(customTsaConfigurationContexts.size() + " contained TSA configs, but trying to access config #" + customTsaConfigurationContextsIndex);
    }
    return customTsaConfigurationContexts.get(customTsaConfigurationContextsIndex++);
  }

  @Override
  public CustomConfigurationContext tsa(Consumer<CustomTsaConfigurationContext> tsa) {
    CustomTsaConfigurationContext customTsaConfigurationContext = new CustomTsaConfigurationContext();
    customTsaConfigurationContexts.add(customTsaConfigurationContext);
    tsa.accept(customTsaConfigurationContext);
    return this;
  }

  @Override
  public TmsConfigurationContext tms() {
    if (customTmsConfigurationContextsIndex >= customTmsConfigurationContexts.size()) {
      throw new IllegalStateException(customTmsConfigurationContexts.size() + " contained TMS configs, but trying to access config #" + customTmsConfigurationContextsIndex);
    }
    return customTmsConfigurationContexts.get(customTmsConfigurationContextsIndex++);
  }

  @Override
  public CustomConfigurationContext tms(Consumer<CustomTmsConfigurationContext> tms) {
    CustomTmsConfigurationContext customTmsConfigurationContext = new CustomTmsConfigurationContext();
    customTmsConfigurationContexts.add(customTmsConfigurationContext);
    tms.accept(customTmsConfigurationContext);
    return this;
  }

  @Override
  public ClientArrayConfigurationContext clientArray() {
    if (customClientArrayConfigurationContextsIndex >= customClientArrayConfigurationContexts.size()) {
      throw new IllegalStateException(customClientArrayConfigurationContexts.size() + " contained client array configs, but trying to access config #" + customClientArrayConfigurationContextsIndex);
    }
    return customClientArrayConfigurationContexts.get(customClientArrayConfigurationContextsIndex++);
  }

  @Override
  public CustomConfigurationContext clientArray(Consumer<CustomClientArrayConfigurationContext> clientArray) {
    CustomClientArrayConfigurationContext customClientArrayConfigurationContext = new CustomClientArrayConfigurationContext();
    customClientArrayConfigurationContexts.add(customClientArrayConfigurationContext);
    clientArray.accept(customClientArrayConfigurationContext);
    return this;
  }

  @Override
  public Set<String> allHostnames() {
    Set<String> hostnames = new HashSet<>();
    for (CustomTsaConfigurationContext customTsaConfigurationContext : customTsaConfigurationContexts) {
      hostnames.addAll(customTsaConfigurationContext.getTopology().getServersHostnames());
    }
    for (CustomTmsConfigurationContext customTmsConfigurationContext : customTmsConfigurationContexts) {
      hostnames.add(customTmsConfigurationContext.getHostname());
    }
    for (CustomClientArrayConfigurationContext customClientArrayConfigurationContext : customClientArrayConfigurationContexts) {
      hostnames.addAll(customClientArrayConfigurationContext.getClientArrayTopology().getClientHostnames());
    }
    return hostnames;
  }
}
