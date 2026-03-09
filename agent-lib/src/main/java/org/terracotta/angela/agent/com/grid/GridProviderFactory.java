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
package org.terracotta.angela.agent.com.grid;

import org.terracotta.angela.common.net.PortAllocator;

import java.util.Collection;
import java.util.ServiceLoader;
import java.util.UUID;

/**
 * SPI for creating {@link GridProvider} instances.
 *
 * <p>Implementations are discovered via {@link ServiceLoader} and selected by
 * {@link #name()} matching the value of the {@code angela.grid.provider} system property.
 *
 * <p>Built-in implementations:
 * <ul>
 *   <li>{@code "ignite2"} &mdash; Apache Ignite 2 (default)</li>
 *   <li>{@code "baton"} &mdash; Baton HTTP-based fabric</li>
 * </ul>
 */
public interface GridProviderFactory {

  /**
   * Returns the short name that identifies this factory (e.g. {@code "ignite2"}, {@code "baton"}).
   * Must match a valid value of {@code angela.grid.provider}.
   */
  String name();

  /**
   * Creates and starts a {@link GridProvider} for the given group.
   *
   * @param group        cluster group UUID
   * @param instanceName logical name for this node (used for logging and AgentID)
   * @param portAllocator allocates free ports for the grid backend
   * @param peers        peer addresses to join (semantics depend on implementation)
   * @return a started {@link GridProvider}
   */
  GridProvider create(UUID group, String instanceName, PortAllocator portAllocator, Collection<String> peers);

  /**
   * Loads all {@link GridProviderFactory} implementations registered via {@link ServiceLoader}
   * and returns the one whose {@link #name()} matches {@code providerName}.
   *
   * @param providerName value of {@code angela.grid.provider}
   * @return the matching factory
   * @throws IllegalArgumentException if no factory is registered for {@code providerName}
   */
  static GridProviderFactory forName(String providerName) {
    for (GridProviderFactory factory : ServiceLoader.load(GridProviderFactory.class)) {
      if (factory.name().equals(providerName)) {
        return factory;
      }
    }
    throw new IllegalArgumentException(
        "No GridProviderFactory registered for provider name '" + providerName + "'. " +
        "Check META-INF/services/" + GridProviderFactory.class.getName() + " and the angela.grid.provider property.");
  }
}
