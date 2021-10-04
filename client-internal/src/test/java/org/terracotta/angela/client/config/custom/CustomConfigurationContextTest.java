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
package org.terracotta.angela.client.config.custom;

import org.junit.Test;
import org.terracotta.angela.client.config.ConfigurationContext;
import org.terracotta.angela.common.tcconfig.License;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * @author Aurelien Broszniowski
 */

public class CustomConfigurationContextTest {

  @Test
  public void testTsaWithoutTopology() {
    try {
      ConfigurationContext configContext = CustomConfigurationContext.customConfigurationContext()
          .tsa(tsa -> {
                final License license = mock(License.class);
                tsa.license(license);
              }
          );
      fail("Exception due to lack of Topology should have been thrown");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }
}
