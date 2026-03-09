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
package org.terracotta.angela.agent.com.grid.baton;

import org.terracotta.angela.agent.com.grid.GridProvider;
import org.terracotta.angela.agent.com.grid.GridProviderFactory;
import org.terracotta.angela.common.net.PortAllocator;

import java.util.Collection;
import java.util.UUID;

/**
 * {@link GridProviderFactory} that creates {@link BatonGridProvider} instances.
 * Registered as the {@code "baton"} backend.
 */
public class BatonGridProviderFactory implements GridProviderFactory {

  @Override
  public String name() {
    return "baton";
  }

  @Override
  public GridProvider create(UUID group, String instanceName, PortAllocator portAllocator, Collection<String> peers) {
    return new BatonGridProvider(group, instanceName, portAllocator, peers);
  }
}
