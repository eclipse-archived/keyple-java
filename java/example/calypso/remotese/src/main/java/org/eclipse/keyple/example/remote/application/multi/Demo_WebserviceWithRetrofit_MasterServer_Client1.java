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
package org.eclipse.keyple.example.remote.application.multi;

import org.eclipse.keyple.example.calypso.common.stub.se.StubCalypsoClassic;
import org.eclipse.keyple.example.remote.application.Demo_Slave;
import org.eclipse.keyple.example.remote.transport.wspolling.client_retrofit.WsPollingRetrofitFactory;
import org.eclipse.keyple.plugin.remotese.transport.factory.TransportFactory;

/**
 * Demo Web Service with Retrofit http client library (Android friendly) The master device uses the
 * websocket master whereas the slave device uses the websocket client
 */
public class Demo_WebserviceWithRetrofit_MasterServer_Client1 {


    public static void main(String[] args) throws Exception {


        final String SERVER_NODE_ID = "RSEServer1";
        final String CLIENT_NODE_ID2 = "RSEClient1";


        final Integer port = 8888;
        final String hostname = "0.0.0.0";
        final String protocol = "http://";

        // Create the procotol factory
        TransportFactory factory =
                new WsPollingRetrofitFactory(SERVER_NODE_ID, protocol, hostname, port);


        // Launch the client 1
        Demo_Slave slave = new Demo_Slave(factory, false, CLIENT_NODE_ID2, SERVER_NODE_ID);

        for (int i = 0; i < 10; i++) {
            // execute Calypso Transaction Scenario
            slave.executeScenario(new StubCalypsoClassic(), false);
            Thread.sleep(5000);
        }

    }
}
