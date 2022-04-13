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
package org.terracotta.angela.common.util;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class RetryUtils {
  public static boolean waitFor(Callable<Boolean> condition, long maxWaitTimeMillis) {
    return waitFor(condition, maxWaitTimeMillis, MILLISECONDS, () -> {
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });
  }

  /**
   * A general-purpose utility for source code, intended as a replacement for Awaitility.
   * Repeatedly polls the {@code condition} with a backoff between multiple poll events, until the specified duration
   * is reached.
   *
   * @param condition       the condition to evaluate
   * @param maxWaitDuration the maximum duration to wait before giving up
   * @param timeUnit        the unit of duration
   * @return {@code true} if the condition was evaluated to true within the given constraints, false otherwise
   */
  public static boolean waitFor(Callable<Boolean> condition, long maxWaitDuration, TimeUnit timeUnit, Runnable failed) {
    TimeBudget timeBudget = new TimeBudget(maxWaitDuration, timeUnit);
    boolean success;
    do {
      try {
        success = condition.call();
        if (!success) {
          failed.run();
        }
      } catch (Exception e) {
        throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
      }
    } while (!success && !Thread.currentThread().isInterrupted() && !timeBudget.isDepleted());
    return success;
  }
}
