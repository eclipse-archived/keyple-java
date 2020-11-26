/* **************************************************************************************
 * Copyright (c) 2019 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.plugin.remotese.nativese.method;

import com.google.gson.JsonObject;
import org.eclipse.keyple.core.seproxy.Reader;
import org.eclipse.keyple.core.seproxy.ReaderPoolPlugin;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderNotFoundException;
import org.eclipse.keyple.plugin.remotese.rm.IRemoteMethodExecutor;
import org.eclipse.keyple.plugin.remotese.rm.RemoteMethodName;
import org.eclipse.keyple.plugin.remotese.transport.json.JsonParser;
import org.eclipse.keyple.plugin.remotese.transport.model.KeypleDto;
import org.eclipse.keyple.plugin.remotese.transport.model.KeypleDtoHelper;
import org.eclipse.keyple.plugin.remotese.transport.model.TransportDto;

public class RmPoolReleaseExecutor implements IRemoteMethodExecutor {

  ReaderPoolPlugin poolPlugin;

  @Override
  public RemoteMethodName getMethodName() {
    return RemoteMethodName.POOL_RELEASE_READER;
  }

  public RmPoolReleaseExecutor(ReaderPoolPlugin poolPlugin) {
    this.poolPlugin = poolPlugin;
  }

  @Override
  public TransportDto execute(TransportDto transportDto) {

    KeypleDto keypleDto = transportDto.getKeypleDTO();
    TransportDto out = null;

    // Extract info from keypleDto
    JsonObject body = JsonParser.getGson().fromJson(keypleDto.getBody(), JsonObject.class);
    String nativeReaderName = body.get("nativeReaderName").getAsString();

    // Find reader to release
    Reader reader = null;
    try {
      reader = poolPlugin.getReader(nativeReaderName);

      // Execute Remote Method
      poolPlugin.releaseReader(reader);

      // Build Response
      JsonObject bodyResp = new JsonObject();
      bodyResp.addProperty("nativeReaderName", reader.getName());

      out =
          transportDto.nextTransportDTO(
              KeypleDtoHelper.buildResponse(
                  getMethodName().getName(),
                  bodyResp.toString(),
                  keypleDto.getSessionId(),
                  reader.getName(),
                  keypleDto.getVirtualReaderName(),
                  keypleDto.getTargetNodeId(),
                  keypleDto.getRequesterNodeId(),
                  keypleDto.getId()));

    } catch (KeypleReaderNotFoundException e) {
      // if an exception occurs, send it into a keypleDto to the Master
      out =
          transportDto.nextTransportDTO(
              KeypleDtoHelper.ExceptionDTO(
                  getMethodName().getName(),
                  e,
                  keypleDto.getSessionId(),
                  reader.getName(),
                  keypleDto.getVirtualReaderName(),
                  keypleDto.getTargetNodeId(),
                  keypleDto.getRequesterNodeId(),
                  keypleDto.getId()));
    }

    return out;
  }
}
