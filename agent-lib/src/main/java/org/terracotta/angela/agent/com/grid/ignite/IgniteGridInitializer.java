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
package org.terracotta.angela.agent.com.grid.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicReference;
import org.apache.ignite.IgniteLock;
import org.terracotta.angela.agent.com.grid.GridInitializer;

import java.util.Objects;

/**
 * Ignite implementation of {@link GridInitializer} that uses distributed locks
 * for cluster-wide coordination.
 */
public class IgniteGridInitializer implements GridInitializer {

  private final Ignite ignite;

  public IgniteGridInitializer(Ignite ignite) {
    this.ignite = Objects.requireNonNull(ignite, "ignite");
  }

  @Override
  public void initializeOnce(String name, Runnable initializer) {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(initializer, "initializer");

    IgniteLock lock = ignite.reentrantLock(name + "@init-lock", true, false, true);
    lock.lock();
    try {
      IgniteAtomicReference<Boolean> initialized = ignite.atomicReference(name + "@initialized", false, true);
      if (Boolean.TRUE.equals(initialized.get())) {
        return;
      }
      initializer.run();
      initialized.set(Boolean.TRUE);
    } finally {
      lock.unlock();
    }
  }
}
