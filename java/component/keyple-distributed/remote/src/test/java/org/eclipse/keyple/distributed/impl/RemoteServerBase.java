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
package org.eclipse.keyple.distributed.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.keyple.distributed.impl.SampleFactory.getDefaultSelectionsResponse;

import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.core.service.event.ObservablePlugin;
import org.eclipse.keyple.core.service.event.ObservableReader;
import org.eclipse.keyple.core.service.event.PluginEvent;
import org.eclipse.keyple.core.service.event.ReaderEvent;
import org.eclipse.keyple.core.util.json.KeypleGsonParser;
import org.eclipse.keyple.distributed.MessageDto;
import org.eclipse.keyple.distributed.ObservableRemoteReaderServer;
import org.eclipse.keyple.distributed.RemoteReaderServer;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class RemoteServerBase {

  static String clientId = "client1";
  static String localReaderName = "localReaderName1";
  static String localPluginName = "localPluginName1";
  static String remotePluginName = "RemoteServerBaseName";
  static String serviceId = "1";
  static RemotePluginServerImplTest.MockUserOutputData userOutputData =
      new RemotePluginServerImplTest.MockUserOutputData();
  static ExecutorService eventNotificationPool = Executors.newCachedThreadPool();

  RemotePluginServerImpl remotePlugin;
  AbstractNode node;
  RemotePluginServerImplTest.MockPluginObserver pluginObserver;
  RemotePluginServerImplTest.MockReaderObserver readerObserver;
  ArgumentCaptor<MessageDto> messageArgumentCaptor;

  /*
   * Private helpers
   */

  class MockReaderObserver implements ObservableReader.ReaderObserver {
    ReaderEvent event;
    Set<String> remoteReaderNames = new HashSet<String>();

    @Override
    public void update(ReaderEvent event) {
      if (remoteReaderNames.add(event.getReaderName())) {
        this.event = event;
      }
      // verify that each event targets a new remote reader
    }

    public void terminateService(Object userOutputData) {
      remotePlugin.terminateService(event.getReaderName(), userOutputData);
    }
  }

  class MockPluginObserver implements ObservablePlugin.PluginObserver {
    PluginEvent event;
    Boolean attachObserver;

    MockPluginObserver(Boolean attachReaderObserver) {
      this.attachObserver = attachReaderObserver;
    }

    @Override
    public void update(PluginEvent event) {
      this.event = event;
      // attach an observer to the RemoteReaderImpl
      RemoteReaderServer remoteReader = remotePlugin.getReader(event.getReaderNames().first());
      if (remoteReader instanceof ObservableRemoteReaderServer && attachObserver) {
        ((ObservableRemoteReaderServer) remoteReader).addObserver(readerObserver);
      }
    }

    public void terminateService(Object userOutputData) {
      remotePlugin.terminateService(event.getReaderNames().first(), userOutputData);
    }
  }

  MessageDto executeRemoteServiceMessage(String sessionId, boolean isObservable) {
    JsonObject body = new JsonObject();
    body.addProperty("serviceId", serviceId);
    body.addProperty("initialCardContent", "");
    body.addProperty("userInputData", "anyObject");
    body.addProperty("isObservable", isObservable);

    return new MessageDto()
        .setSessionId(sessionId)
        .setAction(MessageDto.Action.EXECUTE_REMOTE_SERVICE.name())
        .setClientNodeId(clientId)
        .setLocalReaderName(localReaderName)
        .setBody(body.toString());
  }

  Callable<Boolean> validReaderConnectEvent() {
    return new Callable<Boolean>() {
      @Override
      public Boolean call() {

        return PluginEvent.EventType.READER_CONNECTED.compareTo(pluginObserver.event.getEventType())
                == 0
            && remotePluginName.equals(pluginObserver.event.getPluginName())
            && remotePlugin.getReader(pluginObserver.event.getReaderNames().first()) != null;
      }
    };
  }

  MessageDto readerEventMessage(String sessionId, String remoteReaderName) {
    JsonObject body = new JsonObject();
    body.addProperty("userInputData", "anyObject");
    body.add(
        "readerEvent",
        KeypleGsonParser.getParser()
            .toJsonTree(
                new ReaderEvent(
                    localPluginName,
                    localReaderName,
                    ReaderEvent.EventType.CARD_INSERTED,
                    getDefaultSelectionsResponse())));

    return new MessageDto()
        .setSessionId(sessionId)
        .setAction(MessageDto.Action.READER_EVENT.name())
        .setClientNodeId(clientId)
        .setLocalReaderName(localReaderName)
        .setRemoteReaderName(remoteReaderName)
        .setBody(body.toString());
  }

  Callable<Boolean> validSeInsertedEvent(final String remoteReaderName, final int messageNumber) {
    return new Callable<Boolean>() {
      @Override
      public Boolean call() {
        return ReaderEvent.EventType.CARD_INSERTED.compareTo(readerObserver.event.getEventType())
                == 0
            && remotePluginName.equals(pluginObserver.event.getPluginName())
            && !readerObserver.event.getReaderName().equals(remoteReaderName)
            && readerObserver.remoteReaderNames.size()
                == messageNumber; // event is targeted to the sessionReader
      }
    };
  }

  static class MockUserOutputData {
    String data = "data";
  }

  Answer aVoid() {
    return new Answer() {
      @Override
      public Object answer(InvocationOnMock invocationOnMock) {
        return null;
      }
    };
  }

  void validateTerminateServiceResponse(MessageDto terminateServiceMsg, boolean shouldUnregister) {

    assertThat(terminateServiceMsg.getAction())
        .isEqualTo(MessageDto.Action.TERMINATE_SERVICE.name());
    JsonObject body =
        KeypleGsonParser.getParser().fromJson(terminateServiceMsg.getBody(), JsonObject.class);
    MockUserOutputData userOutputResponse =
        KeypleGsonParser.getParser()
            .fromJson(body.get("userOutputData").getAsString(), MockUserOutputData.class);
    boolean unregisterRemoteReader = body.get("unregisterRemoteReader").getAsBoolean();
    assertThat(userOutputData).isEqualToComparingFieldByFieldRecursively(userOutputResponse);
    assertThat(unregisterRemoteReader).isEqualTo(shouldUnregister);
  }

  void registerSyncPlugin() {
    SmartCardService.getInstance()
        .registerPlugin(
            RemotePluginServerFactory.builder()
                .withPluginName(remotePluginName)
                .withSyncNode()
                .withPluginObserver(pluginObserver)
                .usingDefaultEventNotificationPool()
                .build());
    remotePlugin =
        (RemotePluginServerImpl) SmartCardService.getInstance().getPlugin(remotePluginName);
  }

  void unregisterPlugin() {
    SmartCardService.getInstance().unregisterPlugin(remotePluginName);
  }
}
