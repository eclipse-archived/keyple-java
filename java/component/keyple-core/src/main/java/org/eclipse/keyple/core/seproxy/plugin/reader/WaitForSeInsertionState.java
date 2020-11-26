/* **************************************************************************************
 * Copyright (c) 2019 Calypso Networks Association https://www.calypsonet-asso.org/
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

import java.util.concurrent.ExecutorService;
import org.eclipse.keyple.core.seproxy.event.ObservableReader;
import org.eclipse.keyple.core.seproxy.event.ReaderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wait for card Insertion State
 *
 * <p>The state during which the insertion of a card is expected.
 *
 * <ul>
 *   <li>Upon CARD_INSERTED event, the default selection is processed if required and if the
 *       conditions are met (ALWAYS or CARD_MATCHED) the machine changes state for
 *       WAIT_FOR_SE_PROCESSING.
 *   <li>Upon STOP_DETECT event, the machine changes state for WAIT_FOR_SE_DETECTION.
 *   <li>Upon CARD_REMOVED event, the machine changes state for WAIT_FOR_SE_DETECTION.
 * </ul>
 */
class WaitForSeInsertionState extends AbstractObservableState {

  /** logger */
  private static final Logger logger = LoggerFactory.getLogger(WaitForSeInsertionState.class);

  WaitForSeInsertionState(AbstractObservableLocalReader reader) {
    super(MonitoringState.WAIT_FOR_SE_INSERTION, reader);
  }

  WaitForSeInsertionState(
      AbstractObservableLocalReader reader,
      AbstractMonitoringJob monitoringJob,
      ExecutorService executorService) {
    super(MonitoringState.WAIT_FOR_SE_INSERTION, reader, monitoringJob, executorService);
  }

  @Override
  void onEvent(AbstractObservableLocalReader.InternalEvent event) {
    if (logger.isTraceEnabled()) {
      logger.trace(
          "[{}] onEvent => Event {} received in currentState {}", reader.getName(), event, state);
    }
    /*
     * Process InternalEvent
     */
    switch (event) {
      case CARD_INSERTED:
        // process default selection if any, return an event, can be null
        ReaderEvent seEvent = this.reader.processSeInserted();
        if (seEvent != null) {
          // switch internal state
          switchState(MonitoringState.WAIT_FOR_SE_PROCESSING);
          // notify the external observer of the event
          reader.notifyObservers(seEvent);
        } else {
          // if none event was sent to the application, back to card detection
          // stay in the same state, however switch to WAIT_FOR_SE_INSERTION to relaunch
          // the monitoring job
          if (logger.isTraceEnabled()) {
            logger.trace("[{}] onEvent => Inserted card hasn't matched", reader.getName());
          }
          switchState(MonitoringState.WAIT_FOR_SE_REMOVAL);
        }
        break;

      case STOP_DETECT:
        switchState(MonitoringState.WAIT_FOR_START_DETECTION);
        break;

      case CARD_REMOVED:
        // TODO Check if this case really happens (NFC?)
        // the card has been removed during default selection
        if (reader.getPollingMode() == ObservableReader.PollingMode.REPEATING) {
          switchState(MonitoringState.WAIT_FOR_SE_INSERTION);
        } else {
          switchState(MonitoringState.WAIT_FOR_START_DETECTION);
        }
        break;

      default:
        logger.warn(
            "[{}] Ignore =>  Event {} received in currentState {}", reader.getName(), event, state);
        break;
    }
  }
}
