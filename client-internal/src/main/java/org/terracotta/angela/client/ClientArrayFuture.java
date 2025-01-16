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
package org.terracotta.angela.client;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.terracotta.angela.agent.com.IgniteFutureAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ClientArrayFuture implements Future<Void> {
  private final Collection<Future<Void>> futures;

  public ClientArrayFuture(Collection<Future<Void>> futures) {
    this.futures = futures;
  }

  public Collection<Future<Void>> getFutures() {
    return futures;
  }

  @SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
  @Override
  public Void get(long timeout, TimeUnit unit) throws CancellationException, ExecutionException, InterruptedException, TimeoutException {
    List<Exception> exceptions = new ArrayList<>();
    for (Future<Void> future : futures) {
      try {
        if (timeout == Long.MIN_VALUE && unit == null) {
          future.get();
        } else {
          future.get(timeout, unit);
        }
      } catch (RuntimeException | ExecutionException | InterruptedException | TimeoutException e) {
        exceptions.add(e);
      }
    }
    if (!exceptions.isEmpty()) {
      Exception exception = exceptions.get(0);
      for (int i = 1; i < exceptions.size(); i++) {
        Throwable t = exceptions.get(i);
        if (t instanceof ExecutionException) {
          t = t.getCause();
        }
        if (t instanceof IgniteFutureAdapter.RemoteExecutionException) {
          ((IgniteFutureAdapter.RemoteExecutionException) t).setRemoteStackTraceIndentation(2);
        }
        exception.addSuppressed(t);
      }
      if (exception instanceof RuntimeException) {
        throw (RuntimeException) exception;
      } else if (exception instanceof ExecutionException) {
        throw (ExecutionException) exception;
      } else if (exception instanceof InterruptedException) {
        throw (InterruptedException) exception;
      } else {
        throw (TimeoutException) exception;
      }
    }

    return null;
  }

  @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION")
  @Override
  public Void get() throws CancellationException, ExecutionException, InterruptedException {
    try {
      get(Long.MIN_VALUE, null);
    } catch (TimeoutException te) {
      // This should never happen
      throw new RuntimeException(te);
    }

    return null;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    boolean b = false;
    for (Future<Void> f : futures) {
      b |= f.cancel(mayInterruptIfRunning);
    }
    return b;
  }

  @Override
  public boolean isCancelled() {
    for (Future<Void> f : futures) {
      if (!f.isCancelled()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean isDone() {
    for (Future<Void> f : futures) {
      if (!f.isDone()) {
        return false;
      }
    }
    return true;
  }

  public boolean isAnyDone() {
    return futures.stream()
        .map(Future::isDone)
        .reduce((b1, b2) -> b1 || b2)
        .orElse(true);
  }

  public boolean isAllDone() {
    return futures.stream()
        .map(Future::isDone)
        .reduce((b1, b2) -> b1 && b2)
        .orElse(true);
  }
}
