/*
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License version 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 */

package org.keyple.calypso.commands.po.parser;

import java.nio.ByteBuffer;
import org.keyple.commands.ApduResponseParser;
import org.keyple.seproxy.ApduResponse;

/**
 * The Class PoGetChallengeRespPars. This class provides status code properties and the getters to
 * access to the structured fields of a Get Challenge response.
 *
 * @author Ixxi
 *
 */
public class PoGetChallengeRespPars extends ApduResponseParser {

    /**
     * Instantiates a new PoGetChallengeRespPars.
     *
     * @param response the response from PO Get Challenge APDU Command
     */
    public PoGetChallengeRespPars(ApduResponse response) {
        super(response);
    }

    /**
     * Gets the po challenge.
     *
     * @return the po challenge
     */
    public byte[] getPoChallenge() {
        if (isSuccessful()) {
            return getApduResponse().getBytesBeforeStatus();
        }
        return null;
    }

    public ByteBuffer getPoChallengeV2() {
        return getApduResponse().getDataBeforeStatus();
    }
}
