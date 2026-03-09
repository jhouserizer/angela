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
package org.terracotta.angela;

import org.terracotta.angela.agent.com.grid.baton.BatonGridProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.terracotta.angela.agent.com.AgentID;
import org.terracotta.angela.agent.com.Executor;
import org.terracotta.angela.agent.com.RemoteCallable;
import org.terracotta.angela.agent.com.RemoteRunnable;
import org.terracotta.angela.common.cluster.AtomicCounter;
import org.terracotta.angela.common.cluster.AtomicReference;
import org.terracotta.angela.common.cluster.Barrier;
import org.terracotta.angela.common.cluster.Cluster;
import org.terracotta.angela.common.net.DefaultPortAllocator;
import org.terracotta.angela.common.net.PortAllocator;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Demonstrates that Baton's Angela adapter passes the key ClientIT scenarios.
 *
 * <p>All tests in {@link ClientIT} that are gated by
 * {@code assumeFalse("Cannot run without Ignite when using client jobs", agentID.isLocal())}
 * are skipped when using Angela's built-in {@code IgniteFreeExecutor}. These same
 * scenarios DO work with BatonExecutor because Baton supports local execution,
 * counter/barrier/reference primitives, and proper exception propagation.
 *
 * <p>This class exercises {@link BatonGridProvider} -> {@link Executor} directly,
 * so it is independent of Angela's orchestrator builder and requires no running
 * Ignite cluster.
 */
public class BatonClientIT {

  @Rule
  public Timeout timeout = Timeout.builder().withTimeout(2, TimeUnit.MINUTES).build();

  private PortAllocator portAllocator;
  private BatonGridProvider provider;
  private AgentID agentID;
  private Executor executor;

  @Before
  public void setUp() {
    portAllocator = new DefaultPortAllocator();
    UUID groupId = UUID.randomUUID();
    provider = new BatonGridProvider(groupId, "test", portAllocator, Collections.emptyList());
    agentID = provider.getAgentID();
    executor = provider.createExecutor(groupId, agentID);
  }

  @After
  public void tearDown() {
    provider.close();
    portAllocator.close();
  }

  // AtomicCounter
  // Mirrors testMultipleClientJobsOnSameMachine: multiple concurrent jobs
  // all incrementing the same distributed counter.

  @Test
  public void testCounter_multipleJobsShareState() throws Exception {
    Cluster cluster = executor.getCluster();
    int jobCount = 6;
    List<Future<Void>> futures = new ArrayList<>();
    for (int i = 0; i < jobCount; i++) {
      futures.add(executor.executeAsync(agentID,
          (RemoteRunnable) () -> cluster.atomicCounter("job-count", 0L).incrementAndGet()));
    }
    for (Future<Void> f : futures) {
      f.get(30, TimeUnit.SECONDS);
    }
    assertThat(cluster.atomicCounter("job-count", 0L).get(), is((long) jobCount));
  }

  @Test
  public void testCounter_getAndIncrement() throws Exception {
    Cluster cluster = executor.getCluster();
    executor.executeAsync(agentID,
        (RemoteRunnable) () -> {
          AtomicCounter c = cluster.atomicCounter("gai", 10L);
          c.getAndIncrement();
        }).get(30, TimeUnit.SECONDS);
    assertThat(cluster.atomicCounter("gai", 10L).get(), is(11L));
  }

  @Test
  public void testCounter_compareAndSet() throws Exception {
    Cluster cluster = executor.getCluster();
    executor.executeAsync(agentID,
        (RemoteRunnable) () -> cluster.atomicCounter("cas", 5L).compareAndSet(5L, 99L))
        .get(30, TimeUnit.SECONDS);
    assertThat(cluster.atomicCounter("cas", 5L).get(), is(99L));
  }

  // AtomicReference
  // Mirrors testClientArrayReferenceShared: a job sets references, then the
  // orchestrator reads them back via cluster.atomicReference().

  @Test
  public void testReference_jobSetsOrchReads() throws Exception {
    Cluster cluster = executor.getCluster();
    executor.executeAsync(agentID, (RemoteRunnable) () -> {
      cluster.atomicReference("string", (String) null).set("A");
      cluster.atomicReference("int", 0).compareAndSet(0, 1);
    }).get(30, TimeUnit.SECONDS);

    AtomicReference<String> strRef = cluster.atomicReference("string", "X");
    assertThat(strRef.get(), is("A"));

    AtomicReference<Integer> intRef = cluster.atomicReference("int", 0);
    assertThat(intRef.get(), is(1));
  }

  // Barrier
  // Mirrors testBarrier: two concurrent jobs loop through a barrier, each
  // incrementing a counter after each synchronisation point.

  @Test
  public void testBarrier_twoJobsMeetAtBarrier() throws Exception {
    Cluster cluster = executor.getCluster();
    int clientCount = 2;
    int loopCount = 5;

    RemoteRunnable job = () -> {
      Barrier barrier = cluster.barrier("testBarrier", clientCount);
      for (int i = 0; i < loopCount; i++) {
        barrier.await();
        cluster.atomicCounter("testBarrier-counter", 0L).incrementAndGet();
      }
    };

    List<Future<Void>> futures = new ArrayList<>();
    for (int i = 0; i < clientCount; i++) {
      futures.add(executor.executeAsync(agentID, job));
    }
    for (Future<Void> f : futures) {
      f.get(30, TimeUnit.SECONDS);
    }
    assertThat(cluster.atomicCounter("testBarrier-counter", 0L).get(),
        is((long) clientCount * loopCount));
  }

  // Exception propagation
  // Mirrors testClientArrayExceptionReported: a job throws, the orchestrator
  // receives an ExecutionException wrapping the original message.

  @Test
  public void testException_propagatedToOrchestrator() throws Exception {
    Cluster cluster = executor.getCluster();
    Future<Void> f = executor.executeAsync(agentID, (RemoteRunnable) () -> {
      String msg = "Just Say No (tm) "
          + cluster.atomicCounter("exc-counter", 0L).getAndIncrement();
      throw new RuntimeException(msg);
    });
    try {
      f.get(30, TimeUnit.SECONDS);
      fail("expected ExecutionException");
    } catch (ExecutionException ee) {
      assertThat(exceptionToString(ee), containsString("Just Say No (tm) 0"));
    }
  }

  // Callable returns value
  // Mirrors basic execute-on-all patterns where a job computes and returns a value.

  @Test
  public void testCallable_returnsValue() throws Exception {
    Future<String> f = executor.executeAsync(agentID,
        (RemoteCallable<String>) () -> "hello from baton");
    assertThat(f.get(30, TimeUnit.SECONDS), is("hello from baton"));
  }

  @Test
  public void testCallable_capturesLocalVariable() throws Exception {
    String payload = "captured-" + UUID.randomUUID();
    Future<String> f = executor.executeAsync(agentID,
        (RemoteCallable<String>) () -> payload.toUpperCase());
    assertThat(f.get(30, TimeUnit.SECONDS), is(payload.toUpperCase()));
  }

  // Cluster from orchestrator
  // Mirrors testClientArrayHostNames: cluster.getClientId() is null when
  // obtained from the orchestrator side (not from within a client job).

  @Test
  public void testCluster_nullClientIdFromOrchestrator() {
    assertNull(executor.getCluster().getClientId());
  }

  private static String exceptionToString(Throwable t) {
    StringWriter sw = new StringWriter();
    t.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }
}
