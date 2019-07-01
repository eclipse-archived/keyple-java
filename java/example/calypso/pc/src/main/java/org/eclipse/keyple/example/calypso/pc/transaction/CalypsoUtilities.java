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
package org.eclipse.keyple.example.calypso.pc.transaction;

import static org.eclipse.keyple.calypso.command.sam.SamRevision.C1;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.eclipse.keyple.calypso.transaction.*;
import org.eclipse.keyple.core.selection.SeSelection;
import org.eclipse.keyple.core.seproxy.ChannelState;
import org.eclipse.keyple.core.seproxy.SeReader;
import org.eclipse.keyple.core.seproxy.exception.KeypleBaseException;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException;
import org.eclipse.keyple.example.generic.pc.ReaderUtilities;

public class CalypsoUtilities {
    private static Properties properties;

    static {
        properties = new Properties();

        String propertiesFileName = "config.properties";

        InputStream inputStream =
                CalypsoUtilities.class.getClassLoader().getResourceAsStream(propertiesFileName);

        try {
            if (inputStream != null) {
                properties.load(inputStream);
            } else {
                throw new FileNotFoundException(
                        "property file '" + propertiesFileName + "' not found!");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the default reader for PO communications
     * 
     * @return a SeReader object
     * @throws KeypleBaseException if an error occurred
     */
    public static SeReader getDefaultPoReader() throws KeypleBaseException {
        SeReader poReader =
                ReaderUtilities.getReaderByName(properties.getProperty("po.reader.regex"));

        ReaderUtilities.setContactlessSettings(poReader);

        return poReader;
    }

    /**
     * Get the default reader for SAM communications
     * 
     * @return a {@link SamResource} object
     * @throws KeypleBaseException if an error occurred
     */
    public static SamResource getDefaultSamResource() throws KeypleBaseException {
        SeReader samReader =
                ReaderUtilities.getReaderByName(properties.getProperty("sam.reader.regex"));

        ReaderUtilities.setContactsSettings(samReader);

        /*
         * Open logical channel for the SAM inserted in the reader
         *
         * (We expect the right is inserted)
         */
        SamResource samResource = checkSamAndOpenChannel(samReader);

        return samResource;
    }

    public static SecuritySettings getSecuritySettings() {
        /* define the security parameters to provide when creating PoTransaction */
        return new SecuritySettings();
    }

    /**
     * Check SAM presence and consistency and return a SamResource when everything is correct.
     * <p>
     * Throw an exception if the expected SAM is not available
     *
     * @param samReader the SAM reader
     */
    public static SamResource checkSamAndOpenChannel(SeReader samReader) {
        /*
         * check the availability of the SAM doing a ATR based selection, open its physical and
         * logical channels and keep it open
         */
        SeSelection samSelection = new SeSelection();

        SamSelector samSelector = new SamSelector(C1, ".*", "Selection SAM C1");

        /* Prepare selector, ignore AbstractMatchingSe here */
        samSelection.prepareSelection(new SamSelectionRequest(samSelector, ChannelState.KEEP_OPEN));
        CalypsoSam calypsoSam;

        try {
            calypsoSam = (CalypsoSam) samSelection.processExplicitSelection(samReader)
                    .getActiveSelection().getMatchingSe();
            if (!calypsoSam.isSelected()) {
                throw new IllegalStateException("Unable to open a logical channel for SAM!");
            } else {
            }
        } catch (KeypleReaderException e) {
            throw new IllegalStateException("Reader exception: " + e.getMessage());
        }
        return new SamResource(samReader, calypsoSam);
    }
}
