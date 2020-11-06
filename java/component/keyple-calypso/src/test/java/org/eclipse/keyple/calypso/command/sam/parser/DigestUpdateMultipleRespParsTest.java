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
import org.eclipse.keyple.calypso.command.sam.parser.security.DigestUpdateMultipleRespPars;
import org.eclipse.keyple.core.card.message.ApduResponse;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DigestUpdateMultipleRespParsTest {
  private static final String SW1SW2_KO = "6A83";
  private static final String SW1SW2_OK = "9000";

  @Test(expected = CalypsoSamCommandException.class)
  public void digestUpdateMultipleRespPars_badStatus() {
    DigestUpdateMultipleRespPars digestUpdateMultipleRespPars =
        new DigestUpdateMultipleRespPars(
            new ApduResponse(ByteArrayUtil.fromHex(SW1SW2_KO), null), null);
    digestUpdateMultipleRespPars.checkStatus();
    shouldHaveThrown(CalypsoSamCommandException.class);
  }

  @Test
  public void digestUpdateMultipleRespPars_goodStatus() {
    DigestUpdateMultipleRespPars digestUpdateMultipleRespPars =
        new DigestUpdateMultipleRespPars(
            new ApduResponse(ByteArrayUtil.fromHex(SW1SW2_OK), null), null);
    digestUpdateMultipleRespPars.checkStatus();
  }
}
