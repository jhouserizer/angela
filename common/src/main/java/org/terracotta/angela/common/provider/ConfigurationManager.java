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
package org.terracotta.angela.common.provider;

import org.terracotta.angela.common.net.DisruptionProvider;
import org.terracotta.angela.common.net.Disruptor;
import org.terracotta.angela.common.net.PortAllocator;
import org.terracotta.angela.common.tcconfig.ServerSymbolicName;
import org.terracotta.angela.common.tcconfig.TerracottaServer;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ConfigurationManager extends Serializable {
  long serialVersionUID = 1L;

  void addStripe(TerracottaServer... newServers);

  void removeStripe(int stripeIndex);

  int getStripeIndexOf(UUID serverId);

  List<List<TerracottaServer>> getStripes();

  void addServer(int stripeIndex, TerracottaServer newServer);

  void removeServer(int stripeIndex, int serverIndex);

  TerracottaServer getServer(int stripeIndex, int serverIndex);

  TerracottaServer getServer(UUID serverId);

  List<TerracottaServer> getServers();

  Collection<String> getServersHostnames();

  default void init(PortAllocator portAllocator) {}

  void createDisruptionLinks(TerracottaServer terracottaServer, DisruptionProvider disruptionProvider,
                             Map<ServerSymbolicName, Disruptor> disruptionLinks, Map<ServerSymbolicName, Integer> proxiedPorts,
                             PortAllocator portAllocator);
}
