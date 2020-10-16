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
import org.eclipse.keyple.calypso.command.sam.parser.security.DigestAuthenticateRespPars;
import org.eclipse.keyple.core.card.message.ApduResponse;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DigestAuthenticateRespParsTest {
  private static final String SW1SW2_KO = "6988";
  private static final String SW1SW2_OK = "9000";

  @Test(expected = CalypsoSamCommandException.class)
  public void digestAuthenticateRespPars_badStatus() {
    DigestAuthenticateRespPars digestAuthenticateRespPars =
        new DigestAuthenticateRespPars(
            new ApduResponse(ByteArrayUtil.fromHex(SW1SW2_KO), null), null);
    digestAuthenticateRespPars.checkStatus();
    shouldHaveThrown(CalypsoSamCommandException.class);
  }

  @Test
  public void digestAuthenticateRespPars_goodStatus() {
    DigestAuthenticateRespPars digestAuthenticateRespPars =
        new DigestAuthenticateRespPars(
            new ApduResponse(ByteArrayUtil.fromHex(SW1SW2_OK), null), null);
    digestAuthenticateRespPars.checkStatus();
  }
}
