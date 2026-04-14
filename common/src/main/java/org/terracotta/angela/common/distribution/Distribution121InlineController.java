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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Distribution121InlineController extends Distribution107InlineController {
  private final static Logger LOGGER = LoggerFactory.getLogger(Distribution121InlineController.class);

  public Distribution121InlineController(Distribution distribution) {
    super(distribution);
  }

  @Override
  protected List<String> addOptions(TerracottaServer server, File workingDir) {
    List<String> options = new ArrayList<>();
    Path working = workingDir.toPath();

    options.add(ServerOption.NODE_HOME_DIR.getOption());
    options.add(working.toString());
    options.addAll(getCommonOptions(server, workingDir, ServerOption::getOption));
    addOptionIfNotNull(options, ServerOption.SECURITY_AUDIT_LOG_DIR.getOption(), server.getAuditLogDir());

    LOGGER.debug("Server startup options: {}", options);
    return options;
  }
}
