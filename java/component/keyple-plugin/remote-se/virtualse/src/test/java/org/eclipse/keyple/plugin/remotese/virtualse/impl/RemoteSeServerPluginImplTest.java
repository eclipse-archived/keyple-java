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
package org.eclipse.keyple.plugin.remotese.virtualse.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.eclipse.keyple.core.seproxy.SeProxyService;
import org.eclipse.keyple.core.seproxy.event.*;
import org.eclipse.keyple.plugin.remotese.core.KeypleMessageDto;
import org.eclipse.keyple.plugin.remotese.core.KeypleServerAsync;
import org.eclipse.keyple.plugin.remotese.core.impl.AbstractKeypleNode;
import org.eclipse.keyple.plugin.remotese.virtualse.RemoteSeServerObservableReader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class RemoteSeServerPluginImplTest extends RemoteSeServerBaseTest {

  @Before
  public void setUp() {
    pluginObserver = new MockPluginObserver(true);
    readerObserver = new MockReaderObserver();
    messageArgumentCaptor = ArgumentCaptor.forClass(KeypleMessageDto.class);
    remoteSePlugin =
        Mockito.spy(new RemoteSeServerPluginImpl(remoteSePluginName, eventNotificationPool));
    remoteSePlugin.addObserver(pluginObserver);
    node = Mockito.mock(AbstractKeypleNode.class);
    doReturn(node).when(remoteSePlugin).getNode();
    doAnswer(aVoid()).when(node).sendMessage(messageArgumentCaptor.capture());
  }

  /*
   * Tests
   */

  @Test
  public void registerSyncPlugin() {
    SeProxyService.getInstance()
        .registerPlugin(
            RemoteSeServerPluginFactory.builder()
                .withSyncNode()
                .withPluginObserver(pluginObserver)
                .usingDefaultEventNotificationPool()
                .build());
    assertThat(RemoteSeServerUtils.getSyncPlugin()).isNotNull();
    assertThat(RemoteSeServerUtils.getSyncNode()).isNotNull();

    SeProxyService.getInstance().unregisterPlugin(RemoteSeServerPluginFactory.PLUGIN_NAME_SYNC);
  }

  @Test
  public void registerAsyncPlugin() {
    SeProxyService.getInstance()
        .registerPlugin(
            RemoteSeServerPluginFactory.builder()
                .withAsyncNode(Mockito.mock(KeypleServerAsync.class))
                .withPluginObserver(pluginObserver)
                .usingDefaultEventNotificationPool()
                .build());
    assertThat(RemoteSeServerUtils.getAsyncPlugin()).isNotNull();
    assertThat(RemoteSeServerUtils.getAsyncNode()).isNotNull();

    SeProxyService.getInstance().unregisterPlugin(RemoteSeServerPluginFactory.PLUGIN_NAME_ASYNC);
  }

  @Test
  public void addObserver_removeObserver() {
    assertThat(remoteSePlugin.countObservers()).isEqualTo(1);
    remoteSePlugin.removeObserver(pluginObserver);
    assertThat(remoteSePlugin.countObservers()).isEqualTo(0);
    remoteSePlugin.addObserver(pluginObserver);
    assertThat(remoteSePlugin.countObservers()).isEqualTo(1);
    remoteSePlugin.clearObservers();
    assertThat(remoteSePlugin.countObservers()).isEqualTo(0);
  }

  @Test
  public void onMessage_executeRemoteService_createVirtualReader_shouldRaisePluginEvent() {
    String sessionId = UUID.randomUUID().toString();
    KeypleMessageDto message = executeRemoteServiceMessage(sessionId, false);
    remoteSePlugin.onMessage(message);
    AbstractServerVirtualReader virtualReader =
        (AbstractServerVirtualReader) remoteSePlugin.getReaders().values().iterator().next();
    assertThat(virtualReader).isOfAnyClassIn(ServerVirtualReader.class);
    assertThat(virtualReader).isNotExactlyInstanceOf(ObservableReader.class);
    assertThat(virtualReader.getServiceId()).isEqualTo(serviceId);
    assertThat(virtualReader.getName()).isNotEmpty();
    await().atMost(1, TimeUnit.SECONDS).until(validReaderConnectEvent());
  }

  @Test
  public void
      onMessage_executeRemoteService_createObservableVirtualReader_shouldRaisePluginEvent() {
    String sessionId = UUID.randomUUID().toString();
    KeypleMessageDto message = executeRemoteServiceMessage(sessionId, true);
    remoteSePlugin.onMessage(message);
    ServerVirtualObservableReader virtualReader =
        (ServerVirtualObservableReader) remoteSePlugin.getReaders().values().iterator().next();
    assertThat(virtualReader).isOfAnyClassIn(ServerVirtualObservableReader.class);
    assertThat(virtualReader.getServiceId()).isEqualTo(serviceId);
    assertThat(virtualReader.getName()).isNotEmpty();
    await().atMost(1, TimeUnit.SECONDS).until(validReaderConnectEvent());
  }

  @Test
  public void terminateService_onVirtualReader_withObserver_doNotdeleteVirtualReader() {

    // executing remote service creates a virtual reader
    String sessionId0 = UUID.randomUUID().toString();
    remoteSePlugin.onMessage(executeRemoteServiceMessage(sessionId0, true));
    await().atMost(1, TimeUnit.SECONDS).until(validReaderConnectEvent());

    // terminate service without unregistering reader
    pluginObserver.terminateService(userOutputData);
    KeypleMessageDto terminateServiceMsg = messageArgumentCaptor.getValue();
    assertThat(terminateServiceMsg.getSessionId()).isEqualTo(sessionId0);
    validateTerminateServiceResponse(terminateServiceMsg, false);
    assertThat(remoteSePlugin.getReaders()).hasSize(1);
  }

  @Test
  public void terminateService_onVirtualReader_withoutObserver_shouldDeleteVirtualReader() {
    // do not attach a readerObserver
    remoteSePlugin.clearObservers();
    pluginObserver = new MockPluginObserver(false);
    remoteSePlugin.addObserver(pluginObserver);

    // executing a remote service creates a virtual reader
    String sessionId0 = UUID.randomUUID().toString();
    remoteSePlugin.onMessage(executeRemoteServiceMessage(sessionId0, true));
    await().atMost(1, TimeUnit.SECONDS).until(validReaderConnectEvent());

    // terminate service without unregistering reader
    pluginObserver.terminateService(userOutputData);
    KeypleMessageDto terminateServiceMsg = messageArgumentCaptor.getValue();
    assertThat(terminateServiceMsg.getSessionId()).isEqualTo(sessionId0);
    validateTerminateServiceResponse(terminateServiceMsg, true);
    assertThat(remoteSePlugin.getReaders()).hasSize(0);
  }

  @Test
  public void onEvent_eachEvent_shouldCreateAReader() {
    String sessionId0 = UUID.randomUUID().toString();
    String sessionId1 = UUID.randomUUID().toString();
    String sessionId2 = UUID.randomUUID().toString();

    // execute remote service
    remoteSePlugin.onMessage(executeRemoteServiceMessage(sessionId0, true));
    await().atMost(1, TimeUnit.SECONDS).until(validReaderConnectEvent());

    assertThat(remoteSePlugin.getReaders()).hasSize(1); // one master virtual reader

    // get the virtualReader name
    String virtualReaderName = remoteSePlugin.getReaders().values().iterator().next().getName();

    // send a SE_INSERTED event (1)
    KeypleMessageDto readerEventMessage = readerEventMessage(sessionId1, virtualReaderName);
    remoteSePlugin.onMessage(readerEventMessage);

    // validate the SE_INSERTED event (1)
    await().atMost(1, TimeUnit.SECONDS).until(validSeInsertedEvent(virtualReaderName, 1));

    assertThat(remoteSePlugin.getReaders())
        .hasSize(2); // one master virtual reader, one slave virtual reader

    // send another SE_INSERTED event (2)
    KeypleMessageDto readerEventMessage2 = readerEventMessage(sessionId2, virtualReaderName);
    remoteSePlugin.onMessage(readerEventMessage2);

    // validate the SE_INSERTED event (2)
    await().atMost(1, TimeUnit.SECONDS).until(validSeInsertedEvent(virtualReaderName, 2));

    assertThat(remoteSePlugin.getReaders())
        .hasSize(3); // one master virtual reader, two slave virtual readers
  }

  @Test
  public void terminateService_onSlaveReader_shouldSendOutput_keepVirtualReader() {
    String sessionId0 = UUID.randomUUID().toString();
    String sessionId1 = UUID.randomUUID().toString();

    // execute remote service
    remoteSePlugin.onMessage(executeRemoteServiceMessage(sessionId0, true));
    await().atMost(1, TimeUnit.SECONDS).until(validReaderConnectEvent());

    // get the virtualReader name
    String virtualReaderName = remoteSePlugin.getReaders().values().iterator().next().getName();

    // send a SE_INSERTED event (1)
    KeypleMessageDto readerEventMessage = readerEventMessage(sessionId1, virtualReaderName);
    remoteSePlugin.onMessage(readerEventMessage);

    // validate the SE_INSERTED event (1)
    await().atMost(1, TimeUnit.SECONDS).until(validSeInsertedEvent(virtualReaderName, 1));
    assertThat(remoteSePlugin.getReaders()).hasSize(2); // one virtual reader, one session reader

    // terminate service on slave reader
    readerObserver.terminateService(userOutputData);
    KeypleMessageDto terminateServiceMsg = messageArgumentCaptor.getValue();
    assertThat(terminateServiceMsg.getVirtualReaderName()).isNotEqualTo(virtualReaderName);
    assertThat(terminateServiceMsg.getSessionId()).isEqualTo(sessionId1);
    validateTerminateServiceResponse(terminateServiceMsg, false);
  }

  @Test
  public void terminateService_onSlaveReader_shouldSendOutput_unregisterVirtualReader() {
    String sessionId0 = UUID.randomUUID().toString();
    String sessionId1 = UUID.randomUUID().toString();

    // execute remote service
    remoteSePlugin.onMessage(executeRemoteServiceMessage(sessionId0, true));
    await().atMost(1, TimeUnit.SECONDS).until(validReaderConnectEvent());

    // get the virtualReader name
    String virtualReaderName = remoteSePlugin.getReaders().values().iterator().next().getName();

    // send a SE_INSERTED event (1)
    KeypleMessageDto readerEventMessage = readerEventMessage(sessionId1, virtualReaderName);
    remoteSePlugin.onMessage(readerEventMessage);

    // validate the SE_INSERTED event (1)
    await().atMost(1, TimeUnit.SECONDS).until(validSeInsertedEvent(virtualReaderName, 1));
    assertThat(remoteSePlugin.getReaders()).hasSize(2); // one virtual reader, one session reader

    // remove observers in virtual reader
    RemoteSeServerObservableReader virtualReader =
        (RemoteSeServerObservableReader) remoteSePlugin.getReader(virtualReaderName);
    virtualReader.clearObservers();

    // terminate service on slave reader
    readerObserver.terminateService(userOutputData);
    KeypleMessageDto terminateServiceMsg = messageArgumentCaptor.getValue();
    assertThat(terminateServiceMsg.getVirtualReaderName()).isNotEqualTo(virtualReaderName);
    assertThat(terminateServiceMsg.getSessionId()).isEqualTo(sessionId1);
    validateTerminateServiceResponse(terminateServiceMsg, true);
  }
}
