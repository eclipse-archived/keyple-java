/* **************************************************************************************
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.core.seproxy.plugin.reader;

import java.util.List;
import org.eclipse.keyple.core.seproxy.MultiSeRequestProcessing;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderIOException;
import org.eclipse.keyple.core.seproxy.message.CardRequest;
import org.eclipse.keyple.core.seproxy.message.CardResponse;
import org.eclipse.keyple.core.seproxy.message.ChannelControl;
import org.eclipse.keyple.core.seproxy.message.ProxyReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A generic reader. <code>AbstractReader</code> describes the high-level interface to a {@link
 * ProxyReader} made available by the underlying system.
 *
 * <p><code>AbstractReader</code> defines the minimum required functionality for a local or remote
 * reader and provides some logging facilities.
 */
public abstract class AbstractReader implements ProxyReader {

  /** The name of the reader */
  private final String name;

  /** logger */
  private static final Logger logger = LoggerFactory.getLogger(AbstractReader.class);

  /** Timestamp recorder */
  private long before;

  /** Contains the name of the plugin */
  private final String pluginName;

  /**
   * Reader constructor taking the name of the plugin that instantiated the reader and the name of
   * the reader in argument.
   *
   * <p>Initializes the time measurement log at {@link CardRequest} level. The first measurement
   * gives the time elapsed since the plugin was loaded.
   *
   * @param pluginName A not empty string.
   * @param name A not empty string.
   * @since 0.9
   */
  protected AbstractReader(String pluginName, String name) {

    this.name = name;
    this.pluginName = pluginName;
    if (logger.isDebugEnabled()) {
      this.before = System.nanoTime();
    }
  }

  /**
   * Gets the name of the reader.
   *
   * @return A not empty string.
   */
  public final String getName() {
    return name;
  }

  /**
   * Gets the name of plugin provided in the constructor.
   *
   * @return A not empty string.
   */
  public final String getPluginName() {
    return pluginName;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation of {@link ProxyReader#transmitSeRequests(List, MultiSeRequestProcessing,
   * ChannelControl)} is based on {@link #processSeRequests(List, MultiSeRequestProcessing,
   * ChannelControl)}.<br>
   * It adds a logging of exchanges including a measure of execution time, available at the debug
   * level.
   */
  @Override
  public final List<CardResponse> transmitSeRequests(
      List<CardRequest> cardRequests,
      MultiSeRequestProcessing multiSeRequestProcessing,
      ChannelControl channelControl) {

    List<CardResponse> cardResponse;

    if (logger.isDebugEnabled()) {
      long timeStamp = System.nanoTime();
      long elapsed10ms = (timeStamp - before) / 100000;
      this.before = timeStamp;
      logger.debug(
          "[{}] transmit => SEREQUESTLIST = {}, elapsed {} ms.",
          this.getName(),
          cardRequests,
          elapsed10ms / 10.0);
    }

    try {
      cardResponse = processSeRequests(cardRequests, multiSeRequestProcessing, channelControl);
    } catch (KeypleReaderIOException ex) {
      if (logger.isDebugEnabled()) {
        long timeStamp = System.nanoTime();
        long elapsed10ms = (timeStamp - before) / 100000;
        this.before = timeStamp;
        logger.debug(
            "[{}] transmit => SEREQUESTLIST IO failure. elapsed {}",
            this.getName(),
            elapsed10ms / 10.0);
      } /* Throw an exception with the responses collected so far. */
      throw ex;
    }

    if (logger.isDebugEnabled()) {
      long timeStamp = System.nanoTime();
      long elapsed10ms = (timeStamp - before) / 100000;
      this.before = timeStamp;
      logger.debug(
          "[{}] transmit => CARDRESPONSELIST = {}, elapsed {} ms.",
          this.getName(),
          cardResponse,
          elapsed10ms / 10.0);
    }

    return cardResponse;
  }

  /**
   * This method is the actual implementation of the process of transmitting a list of {@link
   * CardRequest} as defined by {@link ProxyReader#transmitSeRequests(List,
   * MultiSeRequestProcessing, ChannelControl)}.
   *
   * @param cardRequests A not empty list of not null {@link CardRequest}.
   * @param multiSeRequestProcessing The multi card processing flag (must be not null).
   * @param channelControl indicates if the physical channel has to be closed at the end of the
   *     processing (must be not null).
   * @return A not empty response list (can be empty).
   * @throws KeypleReaderIOException if the communication with the reader or the card has failed
   * @throws IllegalArgumentException if one of the arguments is null.
   * @throws IllegalStateException in case of configuration inconsistency.
   * @see ProxyReader#transmitSeRequests(List, MultiSeRequestProcessing, ChannelControl)
   * @since 0.9
   */
  protected abstract List<CardResponse> processSeRequests(
      List<CardRequest> cardRequests,
      MultiSeRequestProcessing multiSeRequestProcessing,
      ChannelControl channelControl);

  /**
   * {@inheritDoc}
   *
   * <p>This implementation of {@link ProxyReader#transmitSeRequest(CardRequest, ChannelControl)} is
   * based on {@link #processSeRequest(CardRequest, ChannelControl)}.<br>
   * It adds a logging of exchanges including a measure of execution time, available at the debug
   * level.
   */
  @Override
  public final CardResponse transmitSeRequest(
      CardRequest cardRequest, ChannelControl channelControl) {

    CardResponse cardResponse;

    if (logger.isDebugEnabled()) {
      long timeStamp = System.nanoTime();
      long elapsed10ms = (timeStamp - before) / 100000;
      this.before = timeStamp;
      logger.debug(
          "[{}] transmit => SEREQUEST = {}, elapsed {} ms.",
          this.getName(),
          cardRequest,
          elapsed10ms / 10.0);
    }

    try {
      cardResponse = processSeRequest(cardRequest, channelControl);
    } catch (KeypleReaderIOException ex) {
      if (logger.isDebugEnabled()) {
        long timeStamp = System.nanoTime();
        long elapsed10ms = (timeStamp - before) / 100000;
        this.before = timeStamp;
        logger.debug(
            "[{}] transmit => SEREQUEST IO failure. elapsed {}",
            this.getName(),
            elapsed10ms / 10.0);
      }
      /* Forward the exception. */
      throw ex;
    }

    if (logger.isDebugEnabled()) {
      long timeStamp = System.nanoTime();
      long elapsed10ms = (timeStamp - before) / 100000;
      this.before = timeStamp;
      logger.debug(
          "[{}] transmit => CARDRESPONSE = {}, elapsed {} ms.",
          this.getName(),
          cardResponse,
          elapsed10ms / 10.0);
    }

    return cardResponse;
  }

  /**
   * This method is the actual implementation of the process of a {@link CardRequest} as defined by
   * {@link ProxyReader#transmitSeRequest(CardRequest, ChannelControl)}
   *
   * @param cardRequest The {@link CardRequest} to be processed (can be null).
   * @return cardResponse A not null {@link CardResponse}.
   * @param channelControl indicates if the physical channel has to be closed at the end of the
   *     processing (must be not null).
   * @throws KeypleReaderIOException if the communication with the reader or the card has failed
   * @throws IllegalArgumentException if one of the arguments is null.
   * @see ProxyReader#transmitSeRequest(CardRequest, ChannelControl)
   * @since 0.9
   */
  protected abstract CardResponse processSeRequest(
      CardRequest cardRequest, ChannelControl channelControl);
}
