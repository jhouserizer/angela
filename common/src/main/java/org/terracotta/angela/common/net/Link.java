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
package org.terracotta.angela.common.net;

import java.net.InetSocketAddress;
import java.util.Objects;

class Link {
  private final InetSocketAddress source;
  private final InetSocketAddress destination;

  public Link(InetSocketAddress source, InetSocketAddress destination) {
    this.source = source;
    this.destination = destination;
  }

  public InetSocketAddress getSource() {
    return source;
  }

  public InetSocketAddress getDestination() {
    return destination;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Link link = (Link) o;
    return Objects.equals(source, link.source) &&
        Objects.equals(destination, link.destination);
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, destination);
  }

  @Override
  public String toString() {
    return "Link{" +
        "source=" + source +
        ", destination=" + destination +
        '}';
  }
}
