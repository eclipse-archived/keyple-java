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
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException;
import org.eclipse.keyple.core.seproxy.message.ProxyReader;
import org.eclipse.keyple.core.seproxy.protocol.TransmissionMode;
import org.eclipse.keyple.plugin.remotese.integration.Integration;
import org.eclipse.keyple.plugin.stub.StubReaderTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit Test Virtual Reader
 */
public class VirtualReaderTest {

    private static final Logger logger = LoggerFactory.getLogger(VirtualReaderTest.class);

    final String NATIVE_READER_NAME = "testStubReader";
    final String CLIENT_NODE_ID = "testClientNodeId";
    final String SERVER_NODE_ID = "testServerNodeId";

    final long RPC_TIMEOUT = 1000;

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test(expected = KeypleReaderException.class)
    public void transmit_timeout() throws KeypleReaderException {
        MasterAPI masterAPI = new MasterAPI(SeProxyService.getInstance(),
                Integration.getFakeDtoNode(), RPC_TIMEOUT);

        RemoteSePlugin plugin = masterAPI.getPlugin();

        ProxyReader reader = plugin.createVirtualReader(CLIENT_NODE_ID, NATIVE_READER_NAME,
                Integration.getFakeDtoNode(), TransmissionMode.CONTACTLESS);

        reader.transmitSet(StubReaderTest.getRequestIsoDepSetSample());
    }


}
