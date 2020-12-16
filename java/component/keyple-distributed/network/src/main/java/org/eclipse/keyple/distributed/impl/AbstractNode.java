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

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import org.eclipse.keyple.distributed.MessageDto;
import org.eclipse.keyple.distributed.NodeCommunicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Abstract Node.
 *
 * @since 1.0
 */
abstract class AbstractNode {

  private static final Logger logger = LoggerFactory.getLogger(AbstractNode.class);

  /**
   * (package-private)<br>
   * The node id.
   *
   * @since 1.0
   */
  final String nodeId;

  /**
   * (package-private)<br>
   * The associated handler.
   *
   * @since 1.0
   */
  final AbstractMessageHandler handler;

  /**
   * (private)<br>
   * Timeout used during awaiting (in milliseconds)
   */
  private final int timeout;

  /**
   * (package-private)<br>
   *
   * @param handler The associated handler (must be not null).
   * @param timeoutInSecond The default timeout (in seconds) to use.
   * @since 1.0
   */
  AbstractNode(AbstractMessageHandler handler, int timeoutInSecond) {
    this.nodeId = UUID.randomUUID().toString();
    this.handler = handler;
    this.timeout = timeoutInSecond * 1000;
  }

  /**
   * (package-private)<br>
   * Open a new session on the endpoint (for internal use only).
   *
   * @param sessionId The session id (must be not empty).
   * @since 1.0
   */
  abstract void openSession(String sessionId);

  /**
   * (package-private)<br>
   * Send a request and return a response (for internal use only).
   *
   * @param msg The message to send (must be not null).
   * @return null if there is no response.
   * @since 1.0
   */
  abstract MessageDto sendRequest(MessageDto msg);

  /**
   * (package-private)<br>
   * Send a message (for internal use only).
   *
   * @param msg The message to send (must be not null).
   * @since 1.0
   */
  abstract void sendMessage(MessageDto msg);

  /**
   * (package-private)<br>
   * Close the session having the provided session id (for internal use only).
   *
   * @param sessionId The session id (must be not empty).
   * @since 1.0
   */
  abstract void closeSession(String sessionId);

  /**
   * (package-private)<br>
   * Close the session silently (without throwing exceptions)
   *
   * @param sessionId The session id (must be not empty).
   * @since 1.0
   */
  void closeSessionSilently(String sessionId) {
    try {
      closeSession(sessionId);
    } catch (RuntimeException e) {
      logger.error(
          "Error during the silent closing of node's session [{}] : {}",
          sessionId,
          e.getMessage(),
          e);
    }
  }

  /**
   * (package-private)<br>
   * The session manager state enum.
   *
   * @since 1.0
   */
  enum SessionManagerState {
    INITIALIZED, //
    OPEN_SESSION_BEGIN, //
    OPEN_SESSION_END, //
    ON_REQUEST, //
    ON_MESSAGE, //
    SEND_REQUEST_BEGIN, //
    SEND_REQUEST_END, //
    SEND_MESSAGE, //
    EXTERNAL_ERROR_OCCURRED, //
    CLOSE_SESSION_BEGIN, //
    CLOSE_SESSION_END, //
    ABORTED_SESSION
  }

  /**
   * (package-private)<br>
   * The inner session manager abstract class.<br>
   * There is one manager by session id.
   *
   * @since 1.0
   */
  abstract class AbstractSessionManager {

    /**
     * (package-private)<br>
     *
     * @since 1.0
     */
    final String sessionId;

    /**
     * (package-private)<br>
     *
     * @since 1.0
     */
    volatile SessionManagerState state;

    /**
     * (package-private)<br>
     *
     * @since 1.0
     */
    MessageDto response;

    /**
     * (package-private)<br>
     *
     * @since 1.0
     */
    Throwable error;

    /**
     * (package-private)<br>
     * Constructor
     *
     * @param sessionId The session id to manage.
     * @since 1.0
     */
    AbstractSessionManager(String sessionId) {
      this.sessionId = sessionId;
      this.state = SessionManagerState.INITIALIZED;
      this.response = null;
      this.error = null;
    }

    /**
     * (package-private)<br>
     * Check if the current state is equal to the target state, else wait until a timeout or to be
     * wake up by another thread.<br>
     *
     * @param targetStates The target states.
     * @since 1.0
     */
    synchronized void waitForState(SessionManagerState... targetStates) {
      for (SessionManagerState targetState : targetStates) {
        if (state == targetState) {
          return;
        }
      }
      checkIfExternalErrorOccurred();
      try {
        long deadline = new Date().getTime() + timeout;
        while (new Date().getTime() < deadline) {
          wait(timeout);
          for (SessionManagerState targetState : targetStates) {
            if (state == targetState) {
              return;
            }
          }
          checkIfExternalErrorOccurred();
        }
        timeoutOccurred();
      } catch (InterruptedException e) {
        logger.error(
            "Unexpected interruption of the task associated with the node's session {}",
            sessionId,
            e);
        Thread.currentThread().interrupt();
      }
    }

    /**
     * (package-private)<br>
     * Check if an external error was received from the endpoint or the handler, regardless to the
     * current state, and then request the cancelling of the session and throws an exception.
     *
     * @throws NodeCommunicationException with the original cause if an error exists.
     * @since 1.0
     */
    abstract void checkIfExternalErrorOccurred();

    /**
     * (package-private)<br>
     * Check if the current state is one of the provided target states.
     *
     * @param targetStates The target states to test.
     * @throws IllegalStateException if the current state does not match any of the states provided.
     * @since 1.0
     */
    void checkState(SessionManagerState... targetStates) {
      for (SessionManagerState targetState : targetStates) {
        if (state == targetState) {
          return;
        }
      }
      throw new IllegalStateException(
          "The status of the node's session manager ["
              + sessionId
              + "] should have been one of "
              + Arrays.toString(targetStates)
              + ", but is currently "
              + state);
    }

    /**
     * (package-private)<br>
     * The timeout case : request the cancelling of the session and throws an exception.
     *
     * @throws NodeCommunicationException the thrown exception.
     * @since 1.0
     */
    void timeoutOccurred() {
      state = SessionManagerState.ABORTED_SESSION;
      logger.error(
          "Timeout occurs for the task associated with the node's session [{}]", sessionId);
      throw new NodeCommunicationException(
          "Timeout occurs for the task associated with the node's session [" + sessionId + "]");
    }
  }
}
