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
package org.eclipse.keyple.core.util.json;


import java.lang.reflect.Type;
import org.eclipse.keyple.core.seproxy.protocol.SeCommonProtocols;
import org.eclipse.keyple.core.seproxy.protocol.SeProtocol;
import com.google.gson.*;

/**
 * Gson Adapter to serialize and deserialize {@link SeProtocol}
 */
public class SeProtocolTypeAdapter
        implements JsonDeserializer<SeProtocol>, JsonSerializer<SeProtocol> {

    @Override
    public JsonElement serialize(SeProtocol src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.getName());
    }

    @Override
    public SeProtocol deserialize(JsonElement json, Type typeOfT,
            JsonDeserializationContext context) throws JsonParseException {

        String value = json.getAsString();

        for (SeCommonProtocols p : SeCommonProtocols.values()) {
            if (p.name().equals(value)) {
                return p;
            }
        }

        throw new JsonParseException("Value of SeProtocol not found : " + value);
    }
}
