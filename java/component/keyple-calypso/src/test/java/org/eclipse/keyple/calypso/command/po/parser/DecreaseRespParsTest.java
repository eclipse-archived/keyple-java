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
package org.eclipse.keyple.calypso.command.po.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.shouldHaveThrown;

import org.eclipse.keyple.calypso.command.po.exception.CalypsoPoCommandException;
import org.eclipse.keyple.core.card.message.ApduResponse;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.junit.Test;

public class DecreaseRespParsTest {
  private static final String SW1SW2_KO = "6A82";
  private static final String SW1SW2_OK = "9000";
  private static final String APDU_BAD_SW1SW2 = SW1SW2_KO;
  private static final int NEW_VALUE = 0x123456;
  private static final String APDU_DECREASE = String.format("%06X", NEW_VALUE) + SW1SW2_OK;

  @Test(expected = CalypsoPoCommandException.class)
  public void decreaseRespPars_badStatus() {
    DecreaseRespPars decreaseRespPars =
        new DecreaseRespPars(new ApduResponse(ByteArrayUtil.fromHex(APDU_BAD_SW1SW2), null), null);
    decreaseRespPars.checkStatus();
    shouldHaveThrown(CalypsoPoCommandException.class);
  }

  @Test
  public void decreaseRespPars_goodStatus() {
    DecreaseRespPars decreaseRespPars =
        new DecreaseRespPars(new ApduResponse(ByteArrayUtil.fromHex(APDU_DECREASE), null), null);
    decreaseRespPars.checkStatus();
  }

  @Test
  public void decreaseRespPars_getNewValue() {
    DecreaseRespPars decreaseRespPars =
        new DecreaseRespPars(new ApduResponse(ByteArrayUtil.fromHex(APDU_DECREASE), null), null);
    decreaseRespPars.checkStatus();
    assertThat(decreaseRespPars.getNewValue()).isEqualTo(NEW_VALUE);
  }
}
