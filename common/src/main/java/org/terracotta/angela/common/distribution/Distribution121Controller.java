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
package org.terracotta.angela.common.distribution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.angela.common.tcconfig.TerracottaServer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static java.io.File.separatorChar;

public class Distribution121Controller extends Distribution107Controller {
  private final static Logger LOGGER = LoggerFactory.getLogger(Distribution121Controller.class);

  public Distribution121Controller(Distribution distribution) {
    super(distribution);
  }

  @Override
  protected List<String> addOptions(TerracottaServer server, File workingDir) throws IOException {
    List<String> options = getCommonOptions(server, workingDir, ServerOption::getOption);

    if (server.getAuditLogDir() != null) {
      options.add(ServerOption.SECURITY_AUDIT_LOG_DIR.getOption());
      String auditPath = workingDir.getAbsolutePath() + separatorChar + "audit-" + server.getServerSymbolicName().getSymbolicName();
      Files.createDirectories(Paths.get(auditPath));
      options.add(auditPath);
    }

    LOGGER.debug("Server startup options: {}", options);
    return options;
  }
}
