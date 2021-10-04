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
package org.terracotta.angela.common.net;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ServiceLoader;

public class DisruptionProviderFactory {
  private static final DisruptionProvider DEFAULT_PROVIDER;

  static {
    DEFAULT_PROVIDER = loadDefault();
  }

  public static DisruptionProvider getDefault() {
    return DEFAULT_PROVIDER;
  }

  private static DisruptionProvider loadDefault() {
    ServiceLoader<DisruptionProvider> serviceLoader = ServiceLoader.load(DisruptionProvider.class);
    Collection<DisruptionProvider> providers = new ArrayList<>();
    for (DisruptionProvider provider : serviceLoader) {
      providers.add(provider);
    }

    if (providers.size() != 1) {
      throw new RuntimeException("Expected number of disrution provider is 1 but found " + providers.size());
    } else {
      return providers.iterator().next();
    }
  }
}
