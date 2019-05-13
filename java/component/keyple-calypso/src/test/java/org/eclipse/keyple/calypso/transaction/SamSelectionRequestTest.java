/********************************************************************************
 * Copyright (c) 2019 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.keyple.calypso.transaction;

import static org.eclipse.keyple.calypso.command.sam.SamRevision.AUTO;
import static org.junit.Assert.*;
import org.eclipse.keyple.core.seproxy.ChannelState;
import org.eclipse.keyple.core.seproxy.message.AnswerToReset;
import org.eclipse.keyple.core.seproxy.message.SeResponse;
import org.eclipse.keyple.core.seproxy.message.SelectionStatus;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.junit.Test;

public class SamSelectionRequestTest {

    @Test
    public void parse() {
        SamSelector samSelector = new SamSelector(AUTO, null, "Dummy SeSelector");
        SamSelectionRequest samSelectionRequest =
                new SamSelectionRequest(samSelector, ChannelState.KEEP_OPEN);
        SelectionStatus selectionStatus = new SelectionStatus(
                new AnswerToReset(ByteArrayUtil.fromHex("3B001122805A0180D002030411223344829000")),
                null, true);
        CalypsoSam calypsoSam =
                samSelectionRequest.parse(new SeResponse(true, true, selectionStatus, null));
        assertEquals(calypsoSam.getSelectionExtraInfo(), "Dummy SeSelector");
    }

    @Test(expected = IllegalStateException.class)
    public void getCommandParser() {
        SamSelector samSelector = new SamSelector(AUTO, null, "Dummy SeSelector");
        SamSelectionRequest samSelectionRequest =
                new SamSelectionRequest(samSelector, ChannelState.KEEP_OPEN);
        SelectionStatus selectionStatus = new SelectionStatus(
                new AnswerToReset(ByteArrayUtil.fromHex("3B001122805A0180D002030411223344829000")),
                null, true);
        samSelectionRequest.getCommandParser(new SeResponse(true, true, selectionStatus, null), 0);
    }
}
