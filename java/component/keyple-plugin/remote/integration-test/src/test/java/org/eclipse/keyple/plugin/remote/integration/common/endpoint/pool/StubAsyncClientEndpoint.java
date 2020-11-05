/* **************************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.plugin.remote.integration.common.endpoint.pool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.eclipse.keyple.core.util.NamedThreadFactory;
import org.eclipse.keyple.plugin.remote.core.KeypleClientAsync;
import org.eclipse.keyple.plugin.remote.core.KeypleMessageDto;
import org.eclipse.keyple.plugin.remote.integration.common.util.JacksonParser;
import org.eclipse.keyple.plugin.remote.virtual.impl.RemotePoolClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Async client endpoint to test {@link
 * org.eclipse.keyple.plugin.remote.virtual.RemotePoolClientPlugin}. Send and receive asynchronously
 * json serialized {@link KeypleMessageDto} with {@link StubAsyncServerEndpoint}.
 */
public class StubAsyncClientEndpoint implements KeypleClientAsync {

  private static final Logger logger = LoggerFactory.getLogger(StubAsyncClientEndpoint.class);
  final StubAsyncServerEndpoint server;
  final ExecutorService taskPool;

  public StubAsyncClientEndpoint(StubAsyncServerEndpoint server) {
    this.server = server;
    this.taskPool = Executors.newCachedThreadPool(new NamedThreadFactory("client-async-pool"));
  }

  /**
   * Receive serialized keyple message dto from the server
   *
   * @param data not null json data
   */
  void onMessage(final String data) {
    taskPool.submit(
        new Runnable() {
          @Override
          public void run() {
            logger.trace("Data received from server : {}", data);
            KeypleMessageDto message = JacksonParser.fromJson(data);
            RemotePoolClientUtils.getAsyncNode().onMessage(message);
          }
        });
  }

  @Override
  public void openSession(String sessionId) {
    RemotePoolClientUtils.getAsyncNode().onOpen(sessionId);
  }

  @Override
  public void sendMessage(final KeypleMessageDto msg) {
    final StubAsyncClientEndpoint thisClient = this;
    // submit task to server
    taskPool.submit(
        new Runnable() {
          @Override
          public void run() {
            String data = JacksonParser.toJson(msg);
            logger.trace("Data sent to server session {} <- {}", msg.getSessionId(), data);
            server.onData(data, thisClient);
          }
        });
  }

  @Override
  public void closeSession(String sessionId) {
    logger.trace("Close session {} to server", sessionId);
    server.close(sessionId);
    RemotePoolClientUtils.getAsyncNode().onClose(sessionId);
  }
}
