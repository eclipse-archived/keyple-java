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
package org.eclipse.keyple.calypso.command.sam.parser;

import static org.assertj.core.api.Assertions.shouldHaveThrown;

import org.eclipse.keyple.calypso.command.sam.exception.CalypsoSamCommandException;
import org.eclipse.keyple.calypso.command.sam.parser.security.UnlockRespPars;
import org.eclipse.keyple.core.card.message.ApduResponse;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UnlockRespParsTest {
  private static final String SW1SW2_KO = "6988";
  private static final String SW1SW2_OK = "9000";

  @Test(expected = CalypsoSamCommandException.class)
  public void unlockRespPars_badStatus() {
    UnlockRespPars unlockRespPars =
        new UnlockRespPars(new ApduResponse(ByteArrayUtil.fromHex(SW1SW2_KO), null), null);
    unlockRespPars.checkStatus();
    shouldHaveThrown(CalypsoSamCommandException.class);
  }

  @Test
  public void unlockRespPars_goodStatus() {
    UnlockRespPars unlockRespPars =
        new UnlockRespPars(new ApduResponse(ByteArrayUtil.fromHex(SW1SW2_OK), null), null);
    unlockRespPars.checkStatus();
  }
}
