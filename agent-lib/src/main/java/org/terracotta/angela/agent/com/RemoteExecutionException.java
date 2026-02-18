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

public class RemoteExecutionException extends Exception {
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
