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
package org.terracotta.angela.client;

import org.terracotta.angela.agent.AgentController;
import org.terracotta.angela.agent.com.AgentExecutor;
import org.terracotta.angela.agent.com.AgentID;
import org.terracotta.angela.agent.com.Executor;
import org.terracotta.angela.agent.kit.LocalKitManager;
import org.terracotta.angela.client.config.ClientArrayConfigurationContext;
import org.terracotta.angela.client.filesystem.RemoteFolder;
import org.terracotta.angela.common.TerracottaCommandLineEnvironment;
import org.terracotta.angela.common.clientconfig.ClientId;
import org.terracotta.angela.common.net.PortAllocator;
import org.terracotta.angela.common.topology.InstanceId;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static org.terracotta.angela.common.AngelaProperties.SKIP_UNINSTALL;

/**
 * @author Aurelien Broszniowski
 */
public class ClientArray implements AutoCloseable {

  private final transient Executor executor;
  private final transient Supplier<InstanceId> clientInstanceIdSupplier;
  private final transient LocalKitManager localKitManager;
  private final transient Map<ClientId, Client> clients = new HashMap<>();
  private final transient ClientArrayConfigurationContext clientArrayConfigurationContext;
  private boolean closed = false;

  ClientArray(Executor executor, PortAllocator portAllocator, Supplier<InstanceId> clientInstanceIdSupplier, ClientArrayConfigurationContext clientArrayConfigurationContext) {
    this.clientArrayConfigurationContext = clientArrayConfigurationContext;
    this.clientInstanceIdSupplier = clientInstanceIdSupplier;
    this.executor = executor;
    this.localKitManager = new LocalKitManager(portAllocator, clientArrayConfigurationContext.getClientArrayTopology().getDistribution());
    installAll();
  }

  private void installAll() {
    clientArrayConfigurationContext.getClientArrayTopology().getClientIds().forEach(this::install);
  }

  private Client install(ClientId clientId) {
    final InstanceId clientInstanceId = clientInstanceIdSupplier.get();
    final TerracottaCommandLineEnvironment environment = clientArrayConfigurationContext.getTerracottaCommandLineEnvironment();
    Client client = Client.spawn(executor, clientInstanceId, clientId, clientArrayConfigurationContext, localKitManager, environment);
    clients.put(clientId, client);
    return client;
  }

  private void uninstallAll() {
    List<Exception> exceptions = new ArrayList<>();

    for (ClientId clientId : clientArrayConfigurationContext.getClientArrayTopology().getClientIds()) {
      try {
        uninstall(clientId);
      } catch (Exception ioe) {
        exceptions.add(ioe);
      }
    }

    if (!exceptions.isEmpty()) {
      RuntimeException ex = new RuntimeException("Error uninstalling some clients");
      exceptions.forEach(ex::addSuppressed);
      throw ex;
    }
  }

  private void uninstall(ClientId clientId) {
    Client client = clients.get(clientId);
    try {
      if (client != null) {
        client.close();
      }
    } finally {
      clients.remove(clientId);
    }
  }

  public Jcmd jcmd(ClientId clientId) {
    return jcmd(clients.get(clientId));
  }

  public Jcmd jcmd(Client client) {
    TerracottaCommandLineEnvironment tcEnv = clientArrayConfigurationContext.getTerracottaCommandLineEnvironment();
    return new Jcmd(executor, client, tcEnv);
  }

  public void stopAll() throws IOException {
    List<Exception> exceptions = new ArrayList<>();

    for (ClientId clientId : clientArrayConfigurationContext.getClientArrayTopology().getClientIds()) {
      try {
        stop(clientId);
      } catch (Exception e) {
        exceptions.add(e);
      }
    }

    if (!exceptions.isEmpty()) {
      IOException ioException = new IOException("Error stopping some clients");
      exceptions.forEach(ioException::addSuppressed);
      throw ioException;
    }
  }

  public void stop(ClientId clientId) {
    Client client = clients.get(clientId);
    if (client != null) {
      client.stop();
    }
  }

  public ClientArrayConfigurationContext getClientArrayConfigurationContext() {
    return clientArrayConfigurationContext;
  }

  public ClientArrayFuture executeOnAll(ClientJob clientJob) {
    return executeOnAll(clientJob, 1);
  }

  public ClientArrayFuture executeOnAll(ClientJob clientJob, int jobsPerClient) {
    List<Future<Void>> futures = new ArrayList<>();
    for (ClientId clientId : clientArrayConfigurationContext.getClientArrayTopology().getClientIds()) {
      for (int i = 1; i <= jobsPerClient; i++) {
        futures.add(executeOn(clientId, clientJob));
      }
    }
    return new ClientArrayFuture(futures);
  }

  public Future<Void> executeOn(ClientId clientId, ClientJob clientJob) {
    return clients.get(clientId).submit(clientId, clientJob);
  }

  public RemoteFolder browse(Client client, String remoteLocation) {
    final InstanceId instanceId = client.getInstanceId();
    final AgentID agentID = executor.getAgentID(client.getHostName());
    final AgentExecutor agentExecutor = executor.forAgent(agentID);
    String clientWorkDir = agentExecutor.execute(() -> AgentController.getInstance().instanceWorkDir(instanceId));
    return new RemoteFolder(agentExecutor, clientWorkDir, remoteLocation);
  }

  public void download(String remoteLocation, File localRootPath) {
    download(remoteLocation, localRootPath.toPath());
  }

  public void download(String remoteLocation, Path localRootPath) {
    List<Exception> exceptions = new ArrayList<>();
    for (Client client : clients.values()) {
      try {
        browse(client, remoteLocation).downloadTo(localRootPath.resolve(client.getSymbolicName()));
      } catch (IOException e) {
        exceptions.add(e);
      }
    }

    if (!exceptions.isEmpty()) {
      RuntimeException re = new RuntimeException("Error downloading cluster monitor remote files");
      exceptions.forEach(re::addSuppressed);
      throw re;
    }
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;

    if (!SKIP_UNINSTALL.getBooleanValue()) {
      uninstallAll();
    }
  }

  public Collection<Client> getClients() {
    return Collections.unmodifiableCollection(this.clients.values());
  }
}
