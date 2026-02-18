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
package org.terracotta.angela.agent.com;

import org.apache.ignite.lang.IgniteRunnable;

/**
 * Backend-neutral interface for remote job execution.
 * Extends {@link IgniteRunnable} temporarily so that lambdas can be passed
 * directly to Ignite's compute API without a wrapping layer that would
 * break peer class loading. This extends-clause will be removed when
 * the Ignite backend is replaced.
 */
@FunctionalInterface
public interface RemoteRunnable extends IgniteRunnable {
}
