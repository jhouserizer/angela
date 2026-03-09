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

import io.baton.Fabric;
import org.terracotta.angela.agent.com.grid.GridInitializer;

import java.util.Objects;

/**
 * Baton-backed implementation of {@link GridInitializer}.
 *
 * <p>Since Baton is coordinator-centric (all state lives in the orchestrator JVM),
 * a CAS on a {@link io.baton.DistributedBoolean} is sufficient to guarantee
 * exactly-once execution without a separate distributed lock.
 */
class BatonGridInitializer implements GridInitializer {

  private final Fabric fabric;

  BatonGridInitializer(Fabric fabric) {
    this.fabric = Objects.requireNonNull(fabric, "fabric");
  }

  @Override
  public void initializeOnce(String name, Runnable initializer) {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(initializer, "initializer");

    // compareAndSet(false -> true) is atomic in Baton.
    // The first caller wins and runs the initializer; all others see true and skip.
    boolean wasUninitialized = fabric.bool(name + "@initialized", false).compareAndSet(false, true);
    if (wasUninitialized) {
      initializer.run();
    }
  }
}
