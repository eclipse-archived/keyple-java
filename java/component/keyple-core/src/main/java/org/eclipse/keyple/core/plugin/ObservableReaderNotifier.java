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
package org.eclipse.keyple.core.plugin;

import org.eclipse.keyple.core.service.event.ObservableReader;
import org.eclipse.keyple.core.service.event.ReaderEvent;

/**
 * Provides the API to notify the observers of an {@link ObservableReader}
 *
 * @since 0.9
 */
public interface ObservableReaderNotifier extends ObservableReader {
  /**
   * Push a ReaderEvent of the {@link ObservableReaderNotifier} to its registered observers.
   *
   * @param event the event (see {@link ReaderEvent})
   * @since 0.9
   */
  void notifyObservers(final ReaderEvent event);
}
