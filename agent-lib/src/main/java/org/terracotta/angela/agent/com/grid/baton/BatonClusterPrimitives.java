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
package org.terracotta.angela.agent.com.grid.baton;

import io.baton.DistributedBarrier;
import io.baton.DistributedBoolean;
import io.baton.DistributedCounter;
import io.baton.DistributedReference;
import io.baton.Fabric;
import io.baton.core.HttpBarrierProxy;
import io.baton.core.HttpBooleanProxy;
import io.baton.core.HttpCounterProxy;
import io.baton.core.HttpReferenceProxy;
import org.terracotta.angela.agent.com.grid.ClusterPrimitives;
import org.terracotta.angela.common.cluster.AtomicBoolean;
import org.terracotta.angela.common.cluster.AtomicCounter;
import org.terracotta.angela.common.cluster.AtomicReference;
import org.terracotta.angela.common.cluster.Barrier;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Baton-backed implementation of Angela's {@link ClusterPrimitives}.
 *
 * <p>Holds the orchestrator base URL and creates HTTP proxy objects for each primitive,
 * so that if they get shipped  to a remote agent (via Baton serialization),
 * the cluster primitives can be reconstructed on the remote side
 * and will call back to the orchestrator.
 */
class BatonClusterPrimitives implements ClusterPrimitives, Serializable {

  private static final long serialVersionUID = 1L;

  private final String orchestratorUrl;

  BatonClusterPrimitives(Fabric fabric) {
    this.orchestratorUrl = fabric.getOrchestratorUrl();
  }

  @Override
  public AtomicCounter atomicCounter(String name, long initialValue) {
    initCounter(name, initialValue);
    DistributedCounter dc = new HttpCounterProxy(orchestratorUrl, name);
    return new AtomicCounter() {
      @Override public long    incrementAndGet()               { return dc.incrementAndGet(); }
      @Override public long    getAndIncrement()               { return dc.getAndIncrement(); }
      @Override public long    get()                           { return dc.get(); }
      @Override public long    getAndSet(long v)               { return dc.getAndSet(v); }
      @Override public boolean compareAndSet(long e, long u)   { return dc.compareAndSet(e, u); }
    };
  }

  @Override
  public AtomicBoolean atomicBoolean(String name, boolean initialValue) {
    initBoolean(name, initialValue);
    DistributedBoolean db = new HttpBooleanProxy(orchestratorUrl, name);
    return new AtomicBoolean() {
      @Override public boolean get()                               { return db.get(); }
      @Override public void    set(boolean v)                      { db.set(v); }
      @Override public boolean getAndSet(boolean v)                { return db.getAndSet(v); }
      @Override public boolean compareAndSet(boolean e, boolean u) { return db.compareAndSet(e, u); }
    };
  }

  @Override
  public <T> AtomicReference<T> atomicReference(String name, T initialValue) {
    initReference(name, (Serializable) initialValue);
    @SuppressWarnings("unchecked")
    DistributedReference<Serializable> dr = new HttpReferenceProxy<>(orchestratorUrl, name);
    return new AtomicReference<T>() {
      @Override @SuppressWarnings("unchecked")
      public T       get()                        { return (T) dr.get(); }
      @Override public void    set(T v)            { dr.set((Serializable) v); }
      @Override public boolean compareAndSet(T e, T u) {
        return dr.compareAndSet((Serializable) e, (Serializable) u);
      }
    };
  }

  @Override
  public Barrier barrier(String name, int parties) {
    DistributedBarrier db = new HttpBarrierProxy(orchestratorUrl, name, parties);
    return new Barrier() {
      @Override public int await() {
        try { return db.await(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
      }
      @Override public int await(long timeout, TimeUnit unit) throws TimeoutException {
        try { return db.await(timeout, unit); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
      }
    };
  }

  // Server-side initialization helpers
  // POST to the orchestrator's HTTP server so that getOrCreate semantics
  // use the supplier initialValue rather than the server default (0 / null).

  private void initCounter(String name, long initialValue) {
    httpPost(orchestratorUrl + "/primitive/counter/" + name + "/get?init=" + initialValue, null);
  }

  private void initBoolean(String name, boolean initialValue) {
    httpPost(orchestratorUrl + "/primitive/boolean/" + name + "/get?init=" + initialValue, null);
  }

  private void initReference(String name, Serializable initialValue) {
    try {
      byte[] body;
      if (initialValue == null) {
        body = new byte[0];
      } else {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(buf)) {
          oos.writeObject(initialValue);
        }
        body = buf.toByteArray();
      }
      httpPost(orchestratorUrl + "/primitive/reference/" + name + "/init", body);
    } catch (IOException e) {
      throw new RuntimeException("Reference init failed: " + name, e);
    }
  }

  private static void httpPost(String url, byte[] body) {
    try {
      HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
      conn.setRequestMethod("POST");
      conn.setDoOutput(true);
      if (body != null && body.length > 0) {
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        conn.getOutputStream().write(body);
      }
      conn.getOutputStream().close();
      int code = conn.getResponseCode();
      if (code != 200) throw new IOException("HTTP " + code + " from " + url);
    } catch (IOException e) {
      throw new RuntimeException("Primitive init HTTP call failed: " + url, e);
    }
  }
}
