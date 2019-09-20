/********************************************************************************
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.keyple.plugin.stub;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.eclipse.keyple.core.seproxy.exception.KeypleChannelStateException;
import org.eclipse.keyple.core.seproxy.exception.KeypleIOReaderException;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException;
import org.eclipse.keyple.core.seproxy.exception.NoStackTraceThrowable;
import org.eclipse.keyple.core.seproxy.plugin.AbstractThreadedLocalReader;
import org.eclipse.keyple.core.seproxy.protocol.SeProtocol;
import org.eclipse.keyple.core.seproxy.protocol.TransmissionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simulates communication with a {@link StubSecureElement}. StubReader is observable, it raises
 * {@link org.eclipse.keyple.core.seproxy.event.ReaderEvent} : SE_INSERTED, SE_REMOVED
 */
final class StubReaderImpl extends AbstractThreadedLocalReader implements StubReader {

    private static final Logger logger = LoggerFactory.getLogger(StubReaderImpl.class);

    private StubSecureElement se;

    private Map<String, String> parameters = new HashMap<String, String>();

    TransmissionMode transmissionMode = TransmissionMode.CONTACTLESS;

    /**
     * Do not use directly
     * 
     * @param name
     */
    StubReaderImpl(String name) {
        super(StubPlugin.PLUGIN_NAME, name);
        threadWaitTimeout = 2000; // time between two events
    }

    StubReaderImpl(String name, TransmissionMode transmissionMode) {
        this(name);
        this.transmissionMode = transmissionMode;
    }

    @Override
    protected byte[] getATR() {
        return se.getATR();
    }

    @Override
    protected boolean isPhysicalChannelOpen() {
        return se != null && se.isPhysicalChannelOpen();
    }

    @Override
    protected void openPhysicalChannel() throws KeypleChannelStateException {
        if (se != null) {
            se.openPhysicalChannel();
        }
    }

    @Override
    public void closePhysicalChannel() throws KeypleChannelStateException {
        if (se != null) {
            se.closePhysicalChannel();
        }
    }

    @Override
    public byte[] transmitApdu(byte[] apduIn) throws KeypleIOReaderException {
        if (se == null) {
            throw new KeypleIOReaderException("No SE available.");
        }
        return se.processApdu(apduIn);
    }

    @Override
    protected boolean protocolFlagMatches(SeProtocol protocolFlag) throws KeypleReaderException {
        boolean result;
        if (se == null) {
            throw new KeypleReaderException("No SE available.");
        }
        // Test protocolFlag to check if ATR based protocol filtering is required
        if (protocolFlag != null) {
            if (!isPhysicalChannelOpen()) {
                openPhysicalChannel();
            }
            // the requestSet will be executed only if the protocol match the requestElement
            String selectionMask = protocolsMap.get(protocolFlag);
            if (selectionMask == null) {
                throw new KeypleReaderException("Target selector mask not found!", null);
            }
            Pattern p = Pattern.compile(selectionMask);
            String protocol = se.getSeProcotol();
            if (!p.matcher(protocol).matches()) {
                logger.trace("[{}] protocolFlagMatches => unmatching SE. PROTOCOLFLAG = {}",
                        this.getName(), protocolFlag);
                result = false;
            } else {
                logger.trace("[{}] protocolFlagMatches => matching SE. PROTOCOLFLAG = {}",
                        this.getName(), protocolFlag);
                result = true;
            }
        } else {
            // no protocol defined returns true
            result = true;
        }
        return result;
    }


    @Override
    protected synchronized boolean checkSePresence() {
        return se != null;
    }

    @Override
    public void setParameter(String name, String value) throws KeypleReaderException {
        if (name.equals(ALLOWED_PARAMETER_1) || name.equals(ALLOWED_PARAMETER_2)) {
            parameters.put(name, value);
        } else if (name.equals(CONTACTS_PARAMETER)) {
            transmissionMode = TransmissionMode.CONTACTS;
        } else if (name.equals(CONTACTLESS_PARAMETER)) {
            transmissionMode = TransmissionMode.CONTACTLESS;
        } else {
            throw new KeypleReaderException("parameter name not supported : " + name);
        }
    }

    @Override
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * @return the current transmission mode
     */
    @Override
    public TransmissionMode getTransmissionMode() {
        return transmissionMode;
    }

    /*
     * STATE CONTROLLERS FOR INSERTING AND REMOVING SECURE ELEMENT
     */

    public synchronized void insertSe(StubSecureElement _se) {
        // logger.info("Insert SE {}", _se);
        /* clean channels status */
        if (isPhysicalChannelOpen()) {
            try {
                closePhysicalChannel();
            } catch (KeypleReaderException e) {
                e.printStackTrace();
            }
        }
        if (_se != null) {
            se = _se;
        }
    }

    public synchronized void removeSe() {
        se = null;
    }

    public StubSecureElement getSe() {
        return se;
    }

    /**
     * This method is called by the monitoring thread to check SE presence
     * 
     * @param timeout the delay in millisecond we wait for a card insertion
     * @return true if the SE is present
     * @throws NoStackTraceThrowable in case of unplugging the reader
     */
    @Override
    protected boolean waitForCardPresent(long timeout) throws NoStackTraceThrowable {
        for (int i = 0; i < timeout / 10; i++) {
            if (checkSePresence()) {
                return true;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                logger.debug("Sleep was interrupted");
            }
        }
        logger.trace("[{}] no card was inserted", this.getName());
        return false;
    }

    /**
     * This method is called by the monitoring thread to check SE absence
     * 
     * @param timeout the delay in millisecond we wait for a card withdrawing
     * @return true if the SE is absent
     * @throws NoStackTraceThrowable in case of unplugging the reader
     */
    @Override
    protected boolean waitForCardAbsent(long timeout) throws NoStackTraceThrowable {
        for (int i = 0; i < timeout / 10; i++) {
            if (!checkSePresence()) {
                logger.trace("[{}] card removed", this.getName());
                return true;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                logger.debug("Sleep was interrupted");
            }
        }
        logger.trace("[{}] no card was removed", this.getName());
        return false;
    }
}
