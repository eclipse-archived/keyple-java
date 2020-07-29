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
package org.eclipse.keyple.core.util.json;

import java.lang.reflect.Type;
import org.eclipse.keyple.core.command.SeCommand;
import org.eclipse.keyple.core.command.exception.KeypleSeCommandException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Force the command field to be serialized as a SeCommand.class
 */
public class KeypleSeCommandExceptionSerializer
        implements JsonSerializer<KeypleSeCommandException> {

    @Override
    public JsonElement serialize(KeypleSeCommandException exception, Type type,
            JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("command",
                jsonSerializationContext.serialize(exception.getCommand(), SeCommand.class));
        jsonObject.addProperty("statusCode", exception.getStatusCode());
        jsonObject.addProperty("message", exception.getMessage());
        return jsonObject;
    }
}
