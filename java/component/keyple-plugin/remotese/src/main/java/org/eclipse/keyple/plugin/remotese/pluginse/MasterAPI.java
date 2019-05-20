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
package org.eclipse.keyple.plugin.remotese.pluginse;

import org.eclipse.keyple.core.seproxy.SeProxyService;
import org.eclipse.keyple.core.seproxy.SeReader;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderNotFoundException;
import org.eclipse.keyple.plugin.remotese.rm.RemoteMethod;
import org.eclipse.keyple.plugin.remotese.transport.*;
import org.eclipse.keyple.plugin.remotese.transport.model.KeypleDto;
import org.eclipse.keyple.plugin.remotese.transport.model.KeypleDtoHelper;
import org.eclipse.keyple.plugin.remotese.transport.model.TransportDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Master API Create/Delete virtual reader based on Commands received from Slave API Propagates
 * Commands to SlaveAPI from virtual reader API
 *
 * Init this API with a {@link DtoSender} of your implementation. Link this API to one your
 * {@link DtoHandler}.
 *
 */
public class MasterAPI implements DtoHandler {

    private static final Logger logger = LoggerFactory.getLogger(MasterAPI.class);

    private final DtoNode dtoTransportNode;
    private final RemoteSePlugin plugin;

    public static final long DEFAULT_RPC_TIMEOUT = 10000;

    /**
     * Build a new MasterAPI, Entry point for incoming DTO in Master Manages RemoteSePlugin
     * lifecycle Manages Master Session Dispatch KeypleDTO
     *
     * @param seProxyService : SeProxyService
     * @param dtoNode : outgoing node to send Dto to Slave
     */
    public MasterAPI(SeProxyService seProxyService, DtoNode dtoNode) {
        this.dtoTransportNode = dtoNode;

        // Instantiate Session Manager
        VirtualReaderSessionFactory sessionManager = new VirtualReaderSessionFactory();

        // Instantiate Plugin
        this.plugin = new RemoteSePlugin(sessionManager, dtoNode, DEFAULT_RPC_TIMEOUT);
        seProxyService.addPlugin(this.plugin);

        // Set this service as the Dto Handler for the node
        this.bindDtoEndpoint(dtoNode);
    }

    /**
     * Build a new MasterAPI, Entry point for incoming DTO in Master Manages RemoteSePlugin
     * lifecycle Manages Master Session Dispatch KeypleDTO
     *
     * @param seProxyService : SeProxyService
     * @param dtoNode : outgoing node to send Dto to Slave
     * @param rpc_timeout : timeout in milliseconds to wait for an answer from slave before throwing
     *        an exception
     */
    public MasterAPI(SeProxyService seProxyService, DtoNode dtoNode, long rpc_timeout) {
        this.dtoTransportNode = dtoNode;

        // Instantiate Session Manager
        VirtualReaderSessionFactory sessionManager = new VirtualReaderSessionFactory();

        // Instantiate Plugin
        this.plugin = new RemoteSePlugin(sessionManager, dtoNode, rpc_timeout);
        seProxyService.addPlugin(this.plugin);

        // Set this service as the Dto Handler for the node
        this.bindDtoEndpoint(dtoNode);
    }

    /**
     * Set this service as the Dto Handler in your {@link DtoNode}
     * 
     * @param node : incoming Dto point
     */
    private void bindDtoEndpoint(DtoNode node) {
        node.setDtoHandler(this);
    }

    /**
     * Retrieve the Rse Plugin
     * 
     * @return the Remote Se Plugin managing the Virtual Readers
     */
    public RemoteSePlugin getPlugin() {
        return plugin;
    }

    /**
     * Handles incoming transportDTO
     * 
     * @param transportDto an incoming TransportDto (embeds a KeypleDto)
     * @return a Response transportDto (can be a NoResponse KeypleDto)
     */
    @Override
    public TransportDto onDTO(TransportDto transportDto) {

        KeypleDto keypleDTO = transportDto.getKeypleDTO();
        RemoteMethod method = RemoteMethod.get(keypleDTO.getAction());
        logger.trace("onDTO, Remote Method called : {} - isRequest : {} - keypleDto : {}", method,
                keypleDTO.isRequest(), KeypleDtoHelper.toJson(keypleDTO));


        switch (method) {

            /*
             * Requests from slave
             */

            case READER_CONNECT:
                if (keypleDTO.isRequest()) {
                    return new RmConnectReaderExecutor(this.plugin, this.dtoTransportNode)
                            .execute(transportDto);
                } else {
                    throw new IllegalStateException(
                            "a READER_CONNECT response has been received by MasterAPI");
                }

            case READER_DISCONNECT:
                if (keypleDTO.isRequest()) {
                    return new RmDisconnectReaderExecutor(this.plugin).execute(transportDto);
                } else {
                    throw new IllegalStateException(
                            "a READER_DISCONNECT response has been received by MasterAPI");
                }


                /*
                 * Notifications from slave
                 */

            case READER_EVENT:
                // process response with the Event Reader RmMethod
                return new RmReaderEventExecutor(plugin).execute(transportDto);

            /*
             * Response from slave
             */

            case READER_TRANSMIT:
                // can be more general
                if (keypleDTO.isRequest()) {
                    throw new IllegalStateException(
                            "a READER_TRANSMIT request has been received by MasterAPI");
                } else {
                    // dispatch dto to the appropriate reader
                    try {
                        // find reader by sessionId
                        VirtualReader reader = getReaderBySessionId(keypleDTO.getSessionId());

                        // process response with the reader rm method engine
                        return reader.getRmTxEngine().onDTO(transportDto);

                    } catch (KeypleReaderNotFoundException e) {
                        // reader not found;
                        throw new IllegalStateException(
                                "Virtual Reader was not found while receiving a transmitSet response",
                                e);
                    } catch (KeypleReaderException e) {
                        // reader not found;
                        throw new IllegalStateException("Readers list has not been initiated", e);
                    }
                }

            case DEFAULT_SELECTION_REQUEST:
                if (keypleDTO.isRequest()) {
                    throw new IllegalStateException(
                            "a READER_TRANSMIT request has been received by MasterAPI");
                } else {
                    // dispatch dto to the appropriate reader
                    try {
                        // find reader by sessionId
                        VirtualReader reader = getReaderBySessionId(keypleDTO.getSessionId());

                        // process response with the reader rmtx engine
                        return reader.getRmTxEngine().onDTO(transportDto);

                    } catch (KeypleReaderNotFoundException e) {
                        // reader not found;
                        throw new IllegalStateException(
                                "Virtual Reader was not found while receiving a transmitSet response",
                                e);
                    } catch (KeypleReaderException e) {
                        // reader not found;
                        throw new IllegalStateException("Readers list has not been initiated", e);
                    }
                }
            default:
                logger.error("Receive a KeypleDto with no recognised action");
                return transportDto.nextTransportDTO(KeypleDtoHelper.NoResponse());
        }
    }



    /**
     * Retrieve reader by its session Id
     * 
     * @param sessionId : sessionId which virtual reader is tight to
     * @return VirtualReader matching the sessionId
     * @throws KeypleReaderNotFoundException : if none reader was found
     */
    private VirtualReader getReaderBySessionId(String sessionId) throws KeypleReaderException {
        for (SeReader reader : plugin.getReaders()) {

            if (((VirtualReader) reader).getSession().getSessionId().equals(sessionId)) {
                return (VirtualReader) reader;
            }
        }
        throw new KeypleReaderNotFoundException(
                "Reader session was not found for session : " + sessionId);
    }

}
