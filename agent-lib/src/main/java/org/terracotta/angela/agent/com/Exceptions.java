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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.ExecutionException;

public class Exceptions {
  public static RuntimeException asRuntime(Throwable e) {
    return asRuntime(null, e);
  }

  public static RuntimeException asRuntime(String msg, Throwable e) {
    if (e instanceof ExecutionException) {
      e = e.getCause();
    }
    if (e instanceof Error) {
      throw (Error) e;
    }
    if (e instanceof RuntimeException) {
      return (RuntimeException) e;
    }
    if (e instanceof IOException) {
      return new UncheckedIOException((IOException) e);
    }
    if (e instanceof InterruptedException) {
      Thread.currentThread().interrupt();
    }
    return msg == null ? new RuntimeException(e) : new RuntimeException(msg, e);
  }
}
