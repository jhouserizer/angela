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
package org.terracotta.angela.agent.cluster;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicReference;

public class AtomicReference<T> {

  private final IgniteAtomicReference<T> igniteReference;

  AtomicReference(Ignite ignite, String name, T initialValue) {
    igniteReference = ignite.atomicReference("Atomic-Reference-" + name, initialValue, true);
  }

  public void set(T value) {
    igniteReference.set(value);
  }

  public boolean compareAndSet(T expect, T update) {
    return igniteReference.compareAndSet(expect, update);
  }

  public T get() {
    return igniteReference.get();
  }
}
