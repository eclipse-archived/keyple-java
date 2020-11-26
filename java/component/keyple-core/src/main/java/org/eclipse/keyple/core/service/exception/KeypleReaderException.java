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
 * The exception {@code KeypleReaderException} is the parent abstract class of all Keyple reader
 * exceptions.
 */
public abstract class KeypleReaderException extends KeypleException {

  /** @param message the message to identify the exception context */
  protected KeypleReaderException(String message) {
    super(message);
  }

  /**
   * Encapsulates a lower level reader exception
   *
   * @param message message to identify the exception context
   * @param cause the cause
   */
  protected KeypleReaderException(String message, Throwable cause) {
    super(message, cause);
  }
}
