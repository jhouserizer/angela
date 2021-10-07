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
package org.terracotta.angela.client.support.junit;

import org.junit.runner.Description;
import org.terracotta.angela.client.AngelaOrchestrator;
import org.terracotta.angela.client.ClusterFactory;
import org.terracotta.angela.client.config.ConfigurationContext;
import org.terracotta.angela.common.net.PortAllocator;

/**
 * @author Mathieu Carbou
 */
public class AngelaOrchestratorRule extends ExtendedTestRule {

  private boolean local;
  private volatile AngelaOrchestrator angelaOrchestrator;

  public AngelaOrchestratorRule local() {
    this.local = true;
    return this;
  }

  public AngelaOrchestrator getAngelaOrchestrator() {
    if (angelaOrchestrator == null) {
      throw new IllegalStateException("Not initialized");
    }
    return angelaOrchestrator;
  }

  @Override
  protected void before(Description description) {
    AngelaOrchestrator.AgentOrchestratorBuilder builder = AngelaOrchestrator.builder();
    if (local) {
      builder = builder.local();
    }
    angelaOrchestrator = builder.build();
  }

  public PortAllocator getPortAllocator() {return getAngelaOrchestrator().getPortAllocator();}

  public ClusterFactory newClusterFactory(String idPrefix, ConfigurationContext configurationContext) {return getAngelaOrchestrator().newClusterFactory(idPrefix, configurationContext);}

  @Override
  protected void after(Description description) {
    angelaOrchestrator.close();
  }
}
