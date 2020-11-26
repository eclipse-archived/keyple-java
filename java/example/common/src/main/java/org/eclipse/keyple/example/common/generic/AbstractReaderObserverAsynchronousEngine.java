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
package org.eclipse.keyple.example.common.generic;

import org.eclipse.keyple.core.service.event.AbstractDefaultSelectionsResponse;
import org.eclipse.keyple.core.service.event.ObservableReader;
import org.eclipse.keyple.core.service.event.ReaderEvent;
import org.eclipse.keyple.core.service.exception.KeypleException;
import org.eclipse.keyple.core.service.exception.KeyplePluginNotFoundException;
import org.eclipse.keyple.core.service.exception.KeypleReaderNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class is intended to be extended by the applications classes in which the card
 * insertion, selection, removal is factorized here.<br>
 * In this implementation of the reader observation, the method {@link
 * ObservableReader.ReaderObserver#update(ReaderEvent)} is processed asynchronously from a separate
 * thread running asynchronously from the monitoring thread.
 */
public abstract class AbstractReaderObserverAsynchronousEngine
    implements ObservableReader.ReaderObserver {

  private static final Logger logger =
      LoggerFactory.getLogger(AbstractReaderObserverAsynchronousEngine.class);

  /**
   * Method to be implemented by the application to handle the CARD_MATCHED reader event.<br>
   * The response to the default selections request is provided in argument.
   *
   * @param defaultSelectionsResponse the default selections response
   */
  protected abstract void processSeMatch(
      AbstractDefaultSelectionsResponse defaultSelectionsResponse);

  /** Method to be implemented by the application to handle the CARD_INSERTED reader event */
  protected abstract void processCardInserted();

  /**
   * Method to be implemented by the application to handle the CARD_REMOVED reader event at the end
   * of the card processing
   */
  protected abstract void processCardRemoved();

  /**
   * Method to be implemented by the application to handle the CARD_REMOVED reader event during the
   * card processing
   */
  protected abstract void processUnexpectedCardRemoval();

  /**
   * This flag helps to determine whether the CARD_REMOVED event was expected or not (case of card
   * withdrawal during processing).
   */
  boolean currentlyProcessingCard = false;

  /**
   * Process {@link #processSeMatch(AbstractDefaultSelectionsResponse)} in a separate thread
   *
   * @param event t
   */
  private void runProcessCardInserted(final ReaderEvent event) {
    /* Run the PO processing asynchronously in a detach thread */
    Thread thread =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                currentlyProcessingCard = true;
                try {
                  processCardInserted(); // optional, to process alternative AID selection
                } catch (KeypleException e) {
                  logger.error("Keyple exception: {}", e.getMessage());
                  /*
                   * Informs the underlying layer of the end of the card processing, in order to
                   * manage the removal sequence.
                   */
                  try {
                    ((ObservableReader) (event.getReader())).finalizeCardProcessing();
                  } catch (KeypleReaderNotFoundException ex) {
                    logger.error("Reader not found exception: {}", ex.getMessage());
                  } catch (KeyplePluginNotFoundException ex) {
                    logger.error("Plugin not found exception: {}", ex.getMessage());
                  }
                }
                currentlyProcessingCard = false;
              }
            });
    thread.start();
  }

  private void runProcessSeMatched(final ReaderEvent event) {
    /* Run the PO processing asynchronously in a detach thread */
    Thread thread =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                currentlyProcessingCard = true;
                try {
                  processSeMatch(event.getDefaultSelectionsResponse()); // to process the
                } catch (KeypleException e) {
                  logger.error("Keyple exception: {}", e.getMessage());
                  /*
                   * Informs the underlying layer of the end of the card processing, in order to
                   * manage the removal sequence.
                   */
                  try {
                    ((ObservableReader) (event.getReader())).finalizeCardProcessing();
                  } catch (KeypleReaderNotFoundException ex) {
                    logger.error("Reader not found exception: {}", ex.getMessage());
                  } catch (KeyplePluginNotFoundException ex) {
                    logger.error("Plugin not found exception: {}", ex.getMessage());
                  }
                }
                currentlyProcessingCard = false;
              }
            });
    thread.start();
  }

  /**
   * Implementation of the {@link ObservableReader.ReaderObserver#update(ReaderEvent)} method.<br>
   * Its role is to call the abstract methods implemented by the application according to the
   * received event.<br>
   * Processing is done asynchronously in a separate thread and any exceptions raised by the
   * application are caught.<br>
   * Note: in the case of CARD_MATCHED, the received event also carries the response to the default
   * selection.
   *
   * @param event the reader event, either CARD_MATCHED, CARD_INSERTED or CARD_REMOVED
   */
  public final void update(final ReaderEvent event) {
    logger.info("New reader event: {}", event.getReaderName());

    switch (event.getEventType()) {
      case CARD_INSERTED:
        runProcessCardInserted(event);
        break;

      case CARD_MATCHED:
        runProcessSeMatched(event);
        break;

      case CARD_REMOVED:
        if (currentlyProcessingCard) {
          processUnexpectedCardRemoval(); // to clean current card processing
          logger.error("Unexpected card Removal");
        } else {
          processCardRemoved();
          if (logger.isInfoEnabled()) {
            logger.info("Waiting for a card...");
          }
        }
        currentlyProcessingCard = false;
        break;
      case UNREGISTERED:
        throw new IllegalStateException(
            "Unexpected error: the reader is no more registered in the SmartcardService.");
    }
  }
}
