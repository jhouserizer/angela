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
package org.terracotta.angela.agent.com;

import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteInterruptedException;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.lang.IgniteFutureTimeoutException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class IgniteFutureAdapter<V> implements Future<V> {
  private final AgentID agentID;
  private final IgniteFuture<V> igniteFuture;

  public IgniteFutureAdapter(AgentID agentID, IgniteFuture<V> igniteFuture) {
    this.agentID = agentID;
    this.igniteFuture = igniteFuture;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return igniteFuture.cancel();
  }

  @Override
  public boolean isCancelled() {
    return igniteFuture.isCancelled();
  }

  @Override
  public boolean isDone() {
    return igniteFuture.isDone();
  }

  @Override
  public V get() throws InterruptedException, ExecutionException {
    try {
      return igniteFuture.get();
    } catch (IgniteInterruptedException iie) {
      throw (InterruptedException) new InterruptedException().initCause(iie);
    } catch (IgniteException ie) {
      RemoteExecutionException ree = lookForRemoteExecutionException(ie);
      if (ree != null) {
        throw new ExecutionException("Job execution failed on agent: " + agentID, ree);
      } else {
        throw new ExecutionException("Job execution failed on agent: " + agentID, ie);
      }
    }
  }

  @Override
  public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    try {
      return igniteFuture.get(timeout, unit);
    } catch (IgniteInterruptedException iie) {
      throw (InterruptedException) new InterruptedException().initCause(iie);
    } catch (IgniteFutureTimeoutException ifte) {
      throw (TimeoutException) new TimeoutException().initCause(ifte);
    } catch (IgniteException ie) {
      RemoteExecutionException ree = lookForRemoteExecutionException(ie);
      if (ree != null) {
        throw new ExecutionException("Job execution failed on agent: " + agentID, ree);
      } else {
        throw new ExecutionException("Job execution failed on agent: " + agentID, ie);
      }
    }
  }

  private static RemoteExecutionException lookForRemoteExecutionException(Throwable t) {
    if (t instanceof RemoteExecutionException) {
      return (RemoteExecutionException) t;
    } else if (t == null) {
      return null;
    } else {
      return lookForRemoteExecutionException(t.getCause());
    }
  }

  public static class RemoteExecutionException extends Exception {
    private static final long serialVersionUID = 1L;
    private final String remoteStackTrace;
    private String tabulation = "\t";

    public RemoteExecutionException(String message, String remoteStackTrace) {
      super(message);
      this.remoteStackTrace = remoteStackTrace;
    }

    @Override
    public String getMessage() {
      return super.getMessage() + "; Remote stack trace is:" + System.lineSeparator() + tabulation + "{{{" + System.lineSeparator() + tabulation + remoteStackTrace() + "}}}";
    }

    private String remoteStackTrace() {
      return remoteStackTrace.replaceAll(System.lineSeparator(), System.lineSeparator() + tabulation);
    }

    public void setRemoteStackTraceIndentation(int indentation) {
      StringBuilder sb = new StringBuilder(indentation);
      for (int i = 0; i < indentation; i++) {
        sb.append('\t');
      }
      tabulation = sb.toString();
    }
  }
}
