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

/**
 * Abstraction for cluster-wide one-time initialization.
 * <p>
 * Implementations ensure that the provided initializer runs exactly once across all nodes
 * in the cluster, even when multiple nodes attempt initialization concurrently.
 * <p>
 * This is typically used for setting up distributed data structures that should only
 * be initialized by one node (e.g., populating initial values, creating shared resources).
 * <p>
 * Note: Instances of this interface are not meant to be serialized across the network.
 * They are created locally on each node and wrap the underlying distributed primitives.
 */
public interface GridInitializer {

  /**
   * Executes the given initializer exactly once cluster-wide.
   * <p>
   * If the initialization for the given name has already been performed (by this node
   * or any other node in the cluster), the initializer will not be executed.
   *
   * @param name        a unique name identifying this initialization (used for distributed coordination)
   * @param initializer the initialization logic to run exactly once
   */
  void initializeOnce(String name, Runnable initializer);
}
