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

import java.net.InetSocketAddress;

/**
 *
 *
 */
public interface DisruptionProvider {


  /**
   * @return true in case of a proxy based provider such as netcrusher or toxiproxy
   */
  boolean isProxyBased();

  /**
   * Create link to disrupt traffic flowing from the given source address to destination address(unidirectional)
   *
   * @param src source address
   * @param dest destination address
   * @return Disruptor link
   */
  Disruptor createLink(InetSocketAddress src, InetSocketAddress dest);


  /**
   * remove link
   *
   * @param link {@link Disruptor}
   */
  void removeLink(Disruptor link);

}
