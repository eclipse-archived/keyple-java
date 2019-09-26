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
package org.eclipse.keyple.plugin.android.omapi;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.keyple.core.seproxy.SeReader;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderNotFoundException;
import org.eclipse.keyple.core.seproxy.plugin.AbstractStaticPlugin;
import org.simalliance.openmobileapi.Reader;
import org.simalliance.openmobileapi.SEService;
import android.util.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads and configures {@link AndroidOmapiReaderImpl} for each SE Reader in the platform TODO : filters
 * readers to load by parameters with a regex
 */
final class AndroidOmapiPluginImpl extends AbstractStaticPlugin implements AndroidOmapiPlugin{


    private static final Logger logger =
            LoggerFactory.getLogger(AndroidOmapiPluginImpl.class);


    private SEService seService;

    /*
    // singleton methods
    private static AndroidOmapiPluginImpl uniqueInstance = null;

    static ISeServiceFactory getSeServiceFactory() {
        return new SeServiceFactoryImpl();
    };
*/


    /**
     * Initialize plugin by connecting to {@link SEService} ; Make sure to instantiate Android Omapi
     * Plugin from a Android Context Application
     */
    AndroidOmapiPluginImpl(SEService seService) {
        super(PLUGIN_NAME);
        this.seService = seService;
        logger.info("OMAPI SEService version: " + seService.getVersion());
    }

/*
    public static AndroidOmapiPluginImpl getInstance() {
        if (uniqueInstance == null) {
            uniqueInstance = new AndroidOmapiPluginImpl();
        }
        return uniqueInstance;
    }
    */



    @Override
    protected SortedSet<SeReader> initNativeReaders() {

        SortedSet<SeReader> readers = new TreeSet<SeReader>();

        if (seService != null && seService.isConnected()) {
            Reader[] omapiReaders = seService.getReaders();

            // no readers found in the environment, don't return any readers for keyple
            if (omapiReaders == null) {
                logger.warn("No readers found");
                return readers;// empty list
            }

            // Build a keyple reader for each readers found by the OMAPI
            for (Reader omapiReader : omapiReaders) {
                logger.debug( "Reader available name : " + omapiReader.getName());
                logger.debug(
                        "Reader available isSePresent : " + omapiReader.isSecureElementPresent());

                // http://seek-for-android.github.io/javadoc/V4.0.0/org/simalliance/openmobileapi/Reader.html
                SeReader seReader =
                        new AndroidOmapiReaderImpl(PLUGIN_NAME, omapiReader, omapiReader.getName());
                readers.add(seReader);
            }

            return readers;

        } else {
            logger.warn( "OMAPI SeService is not connected yet");
            return readers;// empty list
        }

    }

    /**
     * Fetch connected native reader (from third party library) by its name Returns the current
     * {@link SeReader} if it is already listed.
     *
     * @param name reader name to be fetched
     * @return the list of SeReader objects.
     * @throws KeypleReaderNotFoundException if reader is not found
     */
    @Override
    protected SeReader fetchNativeReader(String name)
            throws KeypleReaderNotFoundException {
        return this.getReader(name);
    }

    /**
     * Warning. Do not call this method directly.
     *
     * Invoked by Open Mobile {@link SEService} when connected
     * Instanciates {@link AndroidOmapiReaderImpl} for each SE Reader detected in the platform
     * 
     * @param seService : connected omapi service
    @Override
    public void serviceConnected(SEService seService) {
        Log.i(TAG, "Retrieve available readers...");
        // init readers
        readers = initNativeReaders();
    }
     */

    private Map<String, String> parameters = new HashMap<String, String>();// not in use in this
    // plugin

    @Override
    public Map<String, String> getParameters() {
        logger.warn( "Android OMAPI Plugin does not support parameters, see OMAPINfcReader instead");
        return parameters;
    }

    @Override
    public void setParameter(String key, String value) {
        logger.warn( "Android OMAPI  Plugin does not support parameters, see OMAPINfcReader instead");
        parameters.put(key, value);
    }


}
