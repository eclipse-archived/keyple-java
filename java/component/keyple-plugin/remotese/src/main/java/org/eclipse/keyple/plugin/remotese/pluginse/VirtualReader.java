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
package org.eclipse.keyple.plugin.remotese.pluginse;

import org.eclipse.keyple.core.service.Reader;

/** Define a Virtual Reader (non observable) */
public interface VirtualReader extends Reader {
  /**
   * Name of the Native Reader on the slave device
   *
   * @return local name of the native reader (on slave device)
   */
  String getNativeReaderName();

  /**
   * Return Virtual Reader Session that contains informations about master and slave nodes
   *
   * @return virtual reader session
   */
  public VirtualReaderSession getSession();
}
