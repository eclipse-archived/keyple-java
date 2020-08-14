/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.keyple.plugin.remotese.nativese.impl;

import org.eclipse.keyple.core.seproxy.ReaderPlugin;
import org.eclipse.keyple.core.seproxy.SeProxyService;
import org.eclipse.keyple.core.seproxy.event.ObservableReader;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderNotFoundException;
import org.eclipse.keyple.core.seproxy.message.ProxyReader;
import org.eclipse.keyple.plugin.remotese.core.KeypleMessageDto;
import org.eclipse.keyple.plugin.remotese.core.impl.AbstractKeypleMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * (package-private)<br>
 * Abstract class for all Native SE Services.
 */
abstract class AbstractNativeSeService extends AbstractKeypleMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(AbstractNativeSeService.class);

    /**
     * (protected)<br>
     * Find a local reader among all plugins
     * 
     * @param nativeReaderName name of the reader to be found
     * @return a not null instance
     * @throws KeypleReaderNotFoundException if no reader is found with this name
     * @since 1.0
     */
    protected ProxyReader findLocalReader(String nativeReaderName) {

        if (logger.isTraceEnabled()) {
            logger.trace("Find local reader by name '{}' in {} plugin(s)", nativeReaderName,
                    SeProxyService.getInstance().getPlugins().size());
        }

        for (ReaderPlugin plugin : SeProxyService.getInstance().getPlugins().values()) {
            try {
                if (logger.isTraceEnabled()) {
                    logger.trace("Local reader found '{}' in plugin {}", nativeReaderName,
                            plugin.getName());
                }
                return (ProxyReader) plugin.getReader(nativeReaderName);
            } catch (KeypleReaderNotFoundException e) {
                // reader has not been found in this plugin, continue
            }
        }
        throw new KeypleReaderNotFoundException(nativeReaderName);
    }

    /**
     * (protected)<br>
     * Execute a keypleMessageDto on the local nativeReader, returns the response embedded on a
     * keypleMessageDto ready to be sent back.
     *
     * @param keypleMessageDto not nullable KeypleMessageDto
     * @return a not null instance of the keypleMessageDto response
     * @since 1.0
     *
     */
    protected KeypleMessageDto executeLocally(ProxyReader nativeReader,
            KeypleMessageDto keypleMessageDto) {

        switch (KeypleMessageDto.Action.valueOf(keypleMessageDto.getAction())) {
            case TRANSMIT:
                return new TransmitExecutor(nativeReader).execute(keypleMessageDto);
            case TRANSMIT_SET:
                return new TransmitSetExecutor(nativeReader).execute(keypleMessageDto);
            case SET_DEFAULT_SELECTION:
                return new DefaultSelectionExecutor((ObservableReader) nativeReader)
                        .execute(keypleMessageDto);
            default:
                throw new IllegalArgumentException("keypleMessageDto action value is illegal");
        }
    }
}
