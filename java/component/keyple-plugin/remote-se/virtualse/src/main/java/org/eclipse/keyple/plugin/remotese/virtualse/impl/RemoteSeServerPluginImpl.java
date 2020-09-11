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

import com.google.gson.JsonObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import org.eclipse.keyple.core.seproxy.SeReader;
import org.eclipse.keyple.core.seproxy.event.PluginEvent;
import org.eclipse.keyple.core.seproxy.event.ReaderEvent;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderIOException;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderNotFoundException;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keyple.core.util.json.KeypleJsonParser;
import org.eclipse.keyple.plugin.remotese.core.KeypleMessageDto;
import org.eclipse.keyple.plugin.remotese.virtualse.RemoteSeServerObservableReader;
import org.eclipse.keyple.plugin.remotese.virtualse.RemoteSeServerPlugin;
import org.eclipse.keyple.plugin.remotese.virtualse.RemoteSeServerReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Implementation of RemoteSeServerPlugin
 */
final class RemoteSeServerPluginImpl extends AbstractRemoteSePlugin
    implements RemoteSeServerPlugin {

  private static final Logger logger = LoggerFactory.getLogger(RemoteSeServerPluginImpl.class);

  private final ExecutorService eventNotificationPool;

  /* The observers of this object */
  private final List<PluginObserver> observers;

  /**
   * (package-private)<br>
   * Constructor.
   *
   * <ul>
   *   <li>Instantiates a new ReaderPlugin.
   *   <li>Retrieve the current readers list.
   *   <li>Initialize the list of readers calling the abstract method initNativeReaders.
   *   <li>When readers initialisation failed, a KeypleReaderException is thrown.
   * </ul>
   *
   * @param name The name of the plugin.
   * @throws KeypleReaderException when an issue is raised with reader
   */
  RemoteSeServerPluginImpl(String name, ExecutorService eventNotificationPool) {
    super(name);
    this.eventNotificationPool = eventNotificationPool;
    this.parameters = new HashMap<String, String>();
    this.observers = new ArrayList<PluginObserver>();
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  protected ConcurrentMap<String, SeReader> initNativeReaders() throws KeypleReaderIOException {
    return new ConcurrentHashMap<String, SeReader>();
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  protected void onMessage(KeypleMessageDto message) {
    switch (KeypleMessageDto.Action.valueOf(message.getAction())) {
      case EXECUTE_REMOTE_SERVICE:

        // create a virtual reader from message parameters
        final AbstractServerVirtualReader virtualReader = createMasterReader(message);

        readers.put(virtualReader.getName(), virtualReader);

        notifyObservers(
            new PluginEvent(
                getName(), virtualReader.getName(), PluginEvent.EventType.READER_CONNECTED));
        break;
      case READER_EVENT:
        Assert.getInstance().notNull(message.getVirtualReaderName(), "virtualReaderName");

        ServerVirtualObservableReader delegateVirtualReader = createSlaveReader(message);

        readers.put(delegateVirtualReader.getName(), delegateVirtualReader);

        // notify observers of this event
        ReaderEvent readerEvent =
            KeypleJsonParser.getParser()
                .fromJson(
                    KeypleJsonParser.getParser()
                        .fromJson(message.getBody(), JsonObject.class)
                        .get("readerEvent"),
                    ReaderEvent.class);

        delegateVirtualReader.notifyObservers(
            new ReaderEvent(
                getName(), // plugin name is overwritten
                delegateVirtualReader.getName(), // reader name is overwritten
                readerEvent.getEventType(),
                readerEvent.getDefaultSelectionsResponse()));

        break;
      default:
        throw new IllegalStateException("Message is not supported by Remote Se Plugin " + message);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  public void terminateService(String virtualReaderName, Object userOutputData) {

    AbstractServerVirtualReader virtualReader =
        (AbstractServerVirtualReader) getReader(virtualReaderName);

    // keep virtual reader if observable and has observers
    Boolean unregisterVirtualReader =
        !(virtualReader instanceof RemoteSeServerObservableReader)
            || ((RemoteSeServerObservableReader) virtualReader).countObservers() == 0;

    if (unregisterVirtualReader) {
      // remove virtual readers
      readers.remove(virtualReader.getName());
    }

    JsonObject body = new JsonObject();
    body.addProperty("userOutputData", KeypleJsonParser.getParser().toJson(userOutputData));
    body.addProperty("unregisterVirtualReader", unregisterVirtualReader);

    // Build the message
    KeypleMessageDto message =
        new KeypleMessageDto() //
            .setAction(KeypleMessageDto.Action.TERMINATE_SERVICE.name()) //
            .setVirtualReaderName(virtualReaderName) //
            .setSessionId(virtualReader.getSessionId()) //
            .setBody(body.toString());

    // Send the message
    getNode().sendMessage(message);
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  public RemoteSeServerReader getReader(String name) throws KeypleReaderNotFoundException {
    Assert.getInstance().notNull(name, "reader name");
    RemoteSeServerReader seReader = (RemoteSeServerReader) readers.get(name);
    if (seReader == null) {
      throw new KeypleReaderNotFoundException(name);
    }
    return seReader;
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  public void addObserver(PluginObserver observer) {
    Assert.getInstance().notNull(observer, "Plugin Observer");
    if (observers.add(observer) && logger.isTraceEnabled()) {
      logger.trace(
          "[{}] Added plugin observer '{}'", getName(), observer.getClass().getSimpleName());
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  public void removeObserver(PluginObserver observer) {
    Assert.getInstance().notNull(observer, "Plugin Observer");
    if (observers.remove(observer) && logger.isTraceEnabled()) {
      logger.trace(
          "[{}] Removed plugin observer '{}'", getName(), observer.getClass().getSimpleName());
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  public void clearObservers() {
    observers.clear();
    if (logger.isTraceEnabled()) {
      logger.trace("[{}] Clear reader observers", this.getName());
    }
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  public int countObservers() {
    return observers.size();
  }

  /**
   * (private)<br>
   * Notify observers of an event. Each observer is notified in a separate thread.
   *
   * @param event non nullable instance of event
   */
  private void notifyObservers(final PluginEvent event) {
    for (final PluginObserver observer : observers) {
      eventNotificationPool.execute(
          new Runnable() {
            @Override
            public void run() {
              observer.update(event);
            }
          });
    }
  }

  /**
   * (private)<br>
   * Create a server virtual reader based on incoming message. Can be an observable or not.
   *
   * @param message incoming message
   * @return non null instance of AbstractServerVirtualReader
   */
  private AbstractServerVirtualReader createMasterReader(KeypleMessageDto message) {
    JsonObject body = KeypleJsonParser.getParser().fromJson(message.getBody(), JsonObject.class);
    String serviceId = body.get("serviceId").getAsString();
    String userInputData = body.get("userInputData").getAsString();
    String initialSeContent = body.get("initialSeContent").getAsString();
    boolean isObservable = body.has("isObservable") && body.get("isObservable").getAsBoolean();
    String virtualReaderName = UUID.randomUUID().toString();
    String sessionId = message.getSessionId();

    if (logger.isTraceEnabled()) {
      logger.trace(
          "[{}] Create a virtual reader {} with serviceId:{} and isObservable:{}",
          this.getName(),
          virtualReaderName,
          serviceId,
          isObservable);
    }
    if (isObservable) {
      VirtualObservableReader virtualObservableReader =
          new VirtualObservableReader(
              getName(), virtualReaderName, getNode(), eventNotificationPool);
      virtualObservableReader.setSessionId(sessionId);
      return new ServerVirtualObservableReader(
          virtualObservableReader, serviceId, userInputData, initialSeContent, null);
    } else {
      VirtualReader virtualReader = new VirtualReader(getName(), virtualReaderName, getNode());
      virtualReader.setSessionId(sessionId);
      return new ServerVirtualReader(virtualReader, serviceId, userInputData, initialSeContent);
    }
  }

  /**
   * (private)<br>
   * Create a reader to handle the communication in the session of the event notification
   *
   * @param message incoming reader event message
   * @return non null instance of a ServerVirtualObservableReader
   */
  private ServerVirtualObservableReader createSlaveReader(KeypleMessageDto message) {
    ServerVirtualObservableReader virtualObservableReader =
        (ServerVirtualObservableReader) getReader(message.getVirtualReaderName());

    String userInputData =
        KeypleJsonParser.getParser()
            .fromJson(message.getBody(), JsonObject.class)
            .get("userInputData")
            .getAsString();

    VirtualObservableReader virtualReader =
        new VirtualObservableReader(
            getName(), UUID.randomUUID().toString(), getNode(), eventNotificationPool);
    virtualReader.setSessionId(message.getSessionId());
    // create a temporary virtual reader for this event
    return new ServerVirtualObservableReader(
        virtualReader, null, userInputData, null, virtualObservableReader);
  }
}
