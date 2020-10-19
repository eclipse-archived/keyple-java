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
package org.eclipse.keyple.plugin.remote.nativ.impl;

import org.eclipse.keyple.plugin.remote.core.KeypleClientAsyncNode;
import org.eclipse.keyple.plugin.remote.nativ.NativeClientService;

/**
 * Utility class associated to a {@link NativeClientService}
 *
 * @since 1.0
 */
public class NativeClientUtils {

  /**
   * Get the Native Client Service
   *
   * @return a not null reference
   * @throws IllegalStateException if the service is not initialized.
   * @since 1.0
   */
  public static NativeClientService getService() {
    return getNativeSeClientService();
  }

  /**
   * Get the async node associated to the Native Client Service.
   *
   * @return a not null reference
   * @throws IllegalStateException if the service is not initialized or is not bounded to an async
   *     node.
   * @since 1.0
   */
  public static KeypleClientAsyncNode getAsyncNode() {
    NativeClientServiceImpl service = getNativeSeClientService();
    if (service.getNode() instanceof KeypleClientAsyncNode) {
      return (KeypleClientAsyncNode) service.getNode();
    }
    throw new IllegalStateException("The Native Service is not bounded to an async node");
  }

  /**
   * (private)<br>
   * Get the Native Client Service implementation
   *
   * @return a not null reference
   * @throws IllegalStateException if the service is not initialized.
   */
  private static NativeClientServiceImpl getNativeSeClientService() {
    NativeClientServiceImpl service = NativeClientServiceImpl.getInstance();
    if (service == null) {
      throw new IllegalStateException("The Native Client Service is not initialized");
    }
    return service;
  }
}
