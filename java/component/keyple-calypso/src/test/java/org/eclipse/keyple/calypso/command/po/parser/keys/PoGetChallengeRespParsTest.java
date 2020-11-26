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
package org.eclipse.keyple.calypso.command.po.parser.keys;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.keyple.calypso.command.po.parser.security.PoGetChallengeRespPars;
import org.eclipse.keyple.core.card.message.ApduResponse;
import org.eclipse.keyple.core.card.message.CardResponse;
import org.eclipse.keyple.core.card.message.CardSelectionResponse;
import org.eclipse.keyple.core.card.message.SelectionStatus;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PoGetChallengeRespParsTest {

  @Test
  public void POGetChallengetRespPars() {
    byte[] response =
        new byte[] {
          0x03,
          0x0D,
          0x0E,
          (byte) 0xFA,
          (byte) 0x9C,
          (byte) 0x8C,
          (byte) 0xB7,
          0x27,
          (byte) 0x90,
          0x00
        };
    List<ApduResponse> responses = new ArrayList<ApduResponse>();
    ApduResponse apduResponse = new ApduResponse(response, null);
    responses.add(apduResponse);
    CardSelectionResponse cardSelectionResponse =
        new CardSelectionResponse(
            new SelectionStatus(null, new ApduResponse(ByteArrayUtil.fromHex("9000"), null), true),
            new CardResponse(true, responses));

    PoGetChallengeRespPars apduResponseParser =
        new PoGetChallengeRespPars(
            cardSelectionResponse.getCardResponse().getApduResponses().get(0), null);
    Assert.assertArrayEquals(response, apduResponseParser.getApduResponse().getBytes());
    Assert.assertEquals("Success", apduResponseParser.getStatusInformation());
  }
}
