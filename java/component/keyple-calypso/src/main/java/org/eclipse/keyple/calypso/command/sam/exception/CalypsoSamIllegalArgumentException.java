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
package org.eclipse.keyple.calypso.command.sam.exception;

import org.eclipse.keyple.calypso.command.sam.CalypsoSamCommand;

/**
 * Indicates that the input user data do not allow to build a syntactically correct command.
 *
 * @since 0.9
 */
public final class CalypsoSamIllegalArgumentException extends CalypsoSamCommandException {

  /**
   * @param message the message to identify the exception context
   * @param command the Calypso SAM command
   * @since 0.9
   */
  public CalypsoSamIllegalArgumentException(String message, CalypsoSamCommand command) {
    super(message, command, null);
  }
}
