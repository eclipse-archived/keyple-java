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
package org.eclipse.keyple.example.remote.application;

import org.eclipse.keyple.example.common.calypso.stub.StubCalypsoClassic;
import org.eclipse.keyple.example.remote.transport.websocket.WskFactory;
import org.eclipse.keyple.plugin.remotese.transport.factory.TransportFactory;

/**
 * Demo websocket
 *
 * The master device uses the websocket client whereas the slave device uses the websocket server
 */
public class Demo_Websocket_MasterClient {

    public static void main(String[] args) throws Exception {


        final String CLIENT_NODE_ID = "WskMC1";
        final String SERVER_NODE_ID = "WskMC1Server";


        // Create the procotol factory
        TransportFactory factory = new WskFactory(false, SERVER_NODE_ID); // Web
        // socket

        // Launch the Server thread
        // Server is slave
        SlaveNodeController slave =
                new SlaveNodeController(factory, true, factory.getServerNodeId(), CLIENT_NODE_ID);

        // Launch the client
        // Client is Master
        MasterNodeController master = new MasterNodeController(factory, false, CLIENT_NODE_ID);
        master.boot();

        Thread.sleep(1000);// wait for the server to boot

        // execute Calypso Transaction Scenario
        slave.executeScenario(new StubCalypsoClassic(), true);
    }
}
