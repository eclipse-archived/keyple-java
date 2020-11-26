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
package org.eclipse.keyple.example.common.generic.stub;

import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.eclipse.keyple.plugin.stub.StubSecureElement;

/** Simple contact card Stub (no command) */
public class StubMemoryST25 extends StubSecureElement {

  static final String cardProtocol = "MEMORY_ST25";
  final String ATR_HEX = "3B8F8001804F0CA000000306070007D0020C00B6";

  public StubMemoryST25() {
    /* Get data */
    addHexCommand("FFCA 000000", "8899AABBCCDDEEFF9000");
  }

  @Override
  public byte[] getATR() {
    return ByteArrayUtil.fromHex(ATR_HEX);
  }

  @Override
  public String getCardProtocol() {
    return cardProtocol;
  }
}
