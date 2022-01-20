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
package org.terracotta.angela.common.util;

import org.apache.ignite.Ignite;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.CollectionConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.angela.common.topology.InstanceId;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

public class IgniteCommonHelper {
  private final static Logger logger = LoggerFactory.getLogger(IgniteCommonHelper.class);

  public static BlockingQueue<Object> fileTransferQueue(Ignite ignite, InstanceId instanceId) {
    return ignite.queue(instanceId + "@file-transfer-queue", 100, new CollectionConfiguration());
  }

  public static void displayCluster(Ignite ignite) {
    Collection<ClusterNode> nodes = ignite.cluster().nodes();
    List<Object> nodeNames = nodes.stream().map(clusterNode -> clusterNode.attribute("nodename")).collect(Collectors.toList());
    logger.info("Nodes of the ignite cluster (size = {}): {}", nodes.size(), nodeNames);
  }
}
