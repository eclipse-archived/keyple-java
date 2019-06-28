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
package org.eclipse.keyple.plugin.remotese.pluginse.method;

import org.eclipse.keyple.core.seproxy.event.AbstractDefaultSelectionsRequest;
import org.eclipse.keyple.core.seproxy.event.ObservableReader;
import org.eclipse.keyple.plugin.remotese.rm.RemoteMethod;
import org.eclipse.keyple.plugin.remotese.rm.RemoteMethodTx;
import org.eclipse.keyple.plugin.remotese.transport.json.JsonParser;
import org.eclipse.keyple.plugin.remotese.transport.model.KeypleDto;
import org.eclipse.keyple.plugin.remotese.transport.model.KeypleDtoHelper;
import com.google.gson.JsonObject;

/**
 * Handle the DefautSelectionRequest keypleDTO serialization and deserialization
 */
public class RmSetDefaultSelectionRequestTx extends RemoteMethodTx {

    private final AbstractDefaultSelectionsRequest defaultSelectionsRequest;
    private final ObservableReader.NotificationMode notificationMode;


    @Override
    public RemoteMethod getMethodName() {
        return RemoteMethod.DEFAULT_SELECTION_REQUEST;
    }

    public RmSetDefaultSelectionRequestTx(AbstractDefaultSelectionsRequest defaultSelectionsRequest,
            ObservableReader.NotificationMode notificationMode, String nativeReaderName,
            String virtualReaderName, String sessionId, String slaveNodeId,
            String requesterNodeId) {
        super(sessionId, nativeReaderName, virtualReaderName, slaveNodeId, requesterNodeId);
        this.defaultSelectionsRequest = defaultSelectionsRequest;
        this.notificationMode = notificationMode;

    }

    /*
     * No response is expected from this Rm calls
     */
    @Override
    public Object parseResponse(KeypleDto keypleDto) {
        return new Object();

    }

    @Override
    public KeypleDto dto() {
        JsonObject body = new JsonObject();
        body.addProperty("defaultSelectionsRequest",
                JsonParser.getGson().toJson(defaultSelectionsRequest));
        body.addProperty("notificationMode", notificationMode.getName());

        return KeypleDtoHelper.buildRequest(getMethodName().getName(),
                JsonParser.getGson().toJson(body, JsonObject.class), sessionId, nativeReaderName,
                virtualReaderName, requesterNodeId, targetNodeId, id);

    }
}
