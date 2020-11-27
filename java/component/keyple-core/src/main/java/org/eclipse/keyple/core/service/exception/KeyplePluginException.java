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
package org.eclipse.keyple.core.service.exception;

/**
 * The exception KeyplePluginException is the parent abstract class of all Keyple plugins
 * exceptions.
 */
public abstract class KeyplePluginException extends KeypleException {

  /** @param message the message to identify the exception context */
  protected KeyplePluginException(String message) {
    super(message);
  }

  /**
   * Encapsulates a lower level plugin exception
   *
   * @param message message to identify the exception context
   * @param cause the cause
   */
  protected KeyplePluginException(String message, Throwable cause) {
    super(message, cause);
  }
}
