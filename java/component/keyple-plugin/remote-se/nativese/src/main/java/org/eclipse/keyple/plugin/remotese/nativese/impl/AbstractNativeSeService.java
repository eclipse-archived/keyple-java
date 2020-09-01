/* **************************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.plugin.remotese.nativese.impl;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.eclipse.keyple.core.seproxy.MultiSeRequestProcessing;
import org.eclipse.keyple.core.seproxy.ReaderPlugin;
import org.eclipse.keyple.core.seproxy.SeProxyService;
import org.eclipse.keyple.core.seproxy.event.ObservableReader;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderIOException;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderNotFoundException;
import org.eclipse.keyple.core.seproxy.message.*;
import org.eclipse.keyple.core.seproxy.protocol.SeProtocol;
import org.eclipse.keyple.core.seproxy.protocol.TransmissionMode;
import org.eclipse.keyple.core.util.json.BodyError;
import org.eclipse.keyple.core.util.json.KeypleJsonParser;
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
      logger.trace(
          "Try to find local reader by name '{}' in {} plugin(s)",
          nativeReaderName,
          SeProxyService.getInstance().getPlugins().size());
    }

    for (ReaderPlugin plugin : SeProxyService.getInstance().getPlugins().values()) {
      try {
        if (logger.isTraceEnabled()) {
          logger.trace(
              "Try to find local reader '{}' in plugin {}", nativeReaderName, plugin.getName());
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
   * @param msg The message to process (must be not null).
   * @return a not null reference
   */
  protected KeypleMessageDto executeLocally(ProxyReader nativeReader, KeypleMessageDto msg) {
    return new NativeReaderExecutor(nativeReader, msg).execute();
  }

  /**
   * (private)<br>
   * Nested class used to execute the action on the native reader.
   */
  private static final class NativeReaderExecutor {

    private final ProxyReader reader;
    private final KeypleMessageDto msg;
    private final KeypleMessageDto.Action action;

    private NativeReaderExecutor(ProxyReader reader, KeypleMessageDto msg) {
      this.reader = reader;
      this.msg = msg;
      this.action = KeypleMessageDto.Action.valueOf(msg.getAction());
    }

    /**
     * (private)<br>
     * The main method.
     *
     * @return not null reference which can eventually contain an exception.
     */
    private KeypleMessageDto execute() {

      KeypleMessageDto response;
      try {
        switch (action) {
          case TRANSMIT:
            response = transmit();
            break;
          case TRANSMIT_SET:
            response = transmitSet();
            break;
          case SET_DEFAULT_SELECTION:
            response = setDefaultSelection();
            break;
          case IS_SE_PRESENT:
            response = isSePresent();
            break;
          case ADD_SE_PROTOCOL_SETTING:
            response = addSeProtocolSetting();
            break;
          case SET_SE_PROTOCOL_SETTING:
            response = setSeProtocolSetting();
            break;
          case GET_TRANSMISSION_MODE:
            response = getTransmissionMode();
            break;
          case START_SE_DETECTION:
            response = startSeDetection();
            break;
          case STOP_SE_DETECTION:
            response = stopSeDetection();
            break;
          case FINALIZE_SE_PROCESSING:
            response = finalizeSeProcessing();
            break;
          default:
            throw new IllegalArgumentException(action.name());
        }
      } catch (KeypleReaderIOException e) {
        response =
            new KeypleMessageDto(msg) //
                .setAction(KeypleMessageDto.Action.ERROR.name()) //
                .setBody(KeypleJsonParser.getParser().toJson(new BodyError(e)));
      }
      return response;
    }

    /**
     * (private)<br>
     * Transmit
     *
     * @return a not null reference.
     * @throws KeypleReaderIOException if a reader IO error occurs.
     */
    private KeypleMessageDto transmit() {

      // Extract info from the message
      JsonObject bodyObject =
          KeypleJsonParser.getParser().fromJson(msg.getBody(), JsonObject.class);

      ChannelControl channelControl =
          ChannelControl.valueOf(bodyObject.get("channelControl").getAsString());

      SeRequest seRequest =
          KeypleJsonParser.getParser()
              .fromJson(bodyObject.get("seRequest").getAsString(), SeRequest.class);

      if (logger.isTraceEnabled()) {
        logger.trace(
            "Execute locally seRequest : {} with params {} on reader {}",
            seRequest,
            channelControl,
            reader.getName());
      }

      // Execute the action on the reader
      SeResponse seResponse = reader.transmitSeRequest(seRequest, channelControl);

      // Build response
      String body = KeypleJsonParser.getParser().toJson(seResponse, SeResponse.class);
      return new KeypleMessageDto(msg).setBody(body);
    }

    /**
     * (private)<br>
     * Transmit Set
     *
     * @return a not null reference.
     * @throws KeypleReaderIOException if a reader IO error occurs.
     */
    private KeypleMessageDto transmitSet() {

      // Extract info from the message
      JsonObject bodyJsonO = KeypleJsonParser.getParser().fromJson(msg.getBody(), JsonObject.class);

      List<SeRequest> seRequests =
          KeypleJsonParser.getParser()
              .fromJson(
                  bodyJsonO.get("seRequests").getAsString(),
                  new TypeToken<ArrayList<SeRequest>>() {}.getType());

      MultiSeRequestProcessing multiSeRequestProcessing =
          MultiSeRequestProcessing.valueOf(bodyJsonO.get("multiSeRequestProcessing").getAsString());

      ChannelControl channelControl =
          ChannelControl.valueOf(bodyJsonO.get("channelControl").getAsString());

      if (logger.isTraceEnabled()) {
        logger.trace(
            "Execute locally seRequests : {} with params {} {}",
            seRequests,
            channelControl,
            multiSeRequestProcessing);
      }

      // Execute the action on the reader
      List<SeResponse> seResponses =
          reader.transmitSeRequests(seRequests, multiSeRequestProcessing, channelControl);

      // Build response
      String body =
          KeypleJsonParser.getParser()
              .toJson(seResponses, new TypeToken<ArrayList<SeResponse>>() {}.getType());
      return new KeypleMessageDto(msg).setBody(body);
    }

    /**
     * (private)<br>
     * Set Default Selection
     *
     * @return a not null reference.
     * @throws KeypleReaderIOException if a reader IO error occurs.
     */
    private KeypleMessageDto setDefaultSelection() {

      ObservableReader reader = (ObservableReader) this.reader;

      // Extract info from the message
      JsonObject jsonObject =
          KeypleJsonParser.getParser().fromJson(msg.getBody(), JsonObject.class);

      DefaultSelectionsRequest defaultSelectionsRequest =
          KeypleJsonParser.getParser()
              .fromJson(jsonObject.get("defaultSelectionsRequest"), DefaultSelectionsRequest.class);

      ObservableReader.NotificationMode notificationMode =
          ObservableReader.NotificationMode.get(
              jsonObject.getAsJsonPrimitive("notificationMode").getAsString());

      // Polling Mode can be set or not.
      boolean hasPollingMode = false;
      ObservableReader.PollingMode pollingMode = null;

      String pollingModeJson = jsonObject.get("pollingMode").getAsString();
      if (pollingModeJson != null) {
        hasPollingMode = true;
        pollingMode = ObservableReader.PollingMode.valueOf(pollingModeJson);
      }

      if (logger.isTraceEnabled()) {
        logger.trace(
            "Execute locally set DefaultSelectionExecutor on reader : {} with params {} {} {}",
            reader.getName(),
            defaultSelectionsRequest,
            notificationMode,
            hasPollingMode ? pollingMode : "no-polling-mode");
      }

      // Execute the action on the reader
      if (hasPollingMode) {
        reader.setDefaultSelectionRequest(defaultSelectionsRequest, notificationMode, pollingMode);
      } else {
        reader.setDefaultSelectionRequest(defaultSelectionsRequest, notificationMode);
      }

      // Build response
      return new KeypleMessageDto(msg).setBody(null);
    }

    /**
     * (private)<br>
     * Is SE Present ?
     *
     * @return a not null reference.
     * @throws KeypleReaderIOException if a reader IO error occurs.
     */
    private KeypleMessageDto isSePresent() {

      // Execute the action on the reader
      boolean isSePresent = reader.isSePresent();

      // Build response
      String body = KeypleJsonParser.getParser().toJson(isSePresent, Boolean.class);
      return new KeypleMessageDto(msg).setBody(body);
    }

    /**
     * (private)<br>
     * Add SE Protocol Setting
     *
     * @return a not null reference.
     * @throws KeypleReaderIOException if a reader IO error occurs.
     */
    private KeypleMessageDto addSeProtocolSetting() {

      // Extract info from the message
      JsonObject bodyObject =
          KeypleJsonParser.getParser().fromJson(msg.getBody(), JsonObject.class);

      SeProtocol seProtocol =
          KeypleJsonParser.getParser()
              .fromJson(bodyObject.get("seProtocol").getAsString(), SeProtocol.class);

      String protocolRule = bodyObject.get("protocolRule").getAsString();

      // Execute the action on the reader
      reader.addSeProtocolSetting(seProtocol, protocolRule);

      // Build response
      return new KeypleMessageDto(msg).setBody(null);
    }

    /**
     * (private)<br>
     * Set SE Protocol Setting
     *
     * @return a not null reference.
     * @throws KeypleReaderIOException if a reader IO error occurs.
     */
    private KeypleMessageDto setSeProtocolSetting() {

      // Extract info from the message
      JsonObject bodyObject =
          KeypleJsonParser.getParser().fromJson(msg.getBody(), JsonObject.class);

      Map<SeProtocol, String> protocolSetting =
          KeypleJsonParser.getParser()
              .fromJson(
                  bodyObject.get("protocolSetting").getAsString(),
                  new TypeToken<Map<SeProtocol, String>>() {}.getType());

      // Execute the action on the reader
      reader.setSeProtocolSetting(protocolSetting);

      // Build response
      return new KeypleMessageDto(msg).setBody(null);
    }

    /**
     * (private)<br>
     * Get Transmission Mode
     *
     * @return a not null reference.
     * @throws KeypleReaderIOException if a reader IO error occurs.
     */
    private KeypleMessageDto getTransmissionMode() {

      // Execute the action on the reader
      TransmissionMode transmissionMode = reader.getTransmissionMode();

      // Build response
      String body = KeypleJsonParser.getParser().toJson(transmissionMode, TransmissionMode.class);
      return new KeypleMessageDto(msg).setBody(body);
    }

    /**
     * (private)<br>
     * Start SE Detection
     *
     * @return a not null reference.
     * @throws KeypleReaderIOException if a reader IO error occurs.
     */
    private KeypleMessageDto startSeDetection() {

      // Extract info from the message
      JsonObject body = KeypleJsonParser.getParser().fromJson(msg.getBody(), JsonObject.class);

      ObservableReader.PollingMode pollingMode =
          KeypleJsonParser.getParser()
              .fromJson(body.get("pollingMode"), ObservableReader.PollingMode.class);

      // Execute the action on the reader
      ((ObservableReader) reader).startSeDetection(pollingMode);

      return new KeypleMessageDto(msg).setBody(null);
    }

    /**
     * (private)<br>
     * Stop SE Detection
     *
     * @return a not null reference.
     * @throws KeypleReaderIOException if a reader IO error occurs.
     */
    private KeypleMessageDto stopSeDetection() {

      // Execute the action on the reader
      ((ObservableReader) reader).stopSeDetection();

      return new KeypleMessageDto(msg).setBody(null);
    }

    /**
     * (private)<br>
     * Finalize SE Processing
     *
     * @return a not null reference.
     * @throws KeypleReaderIOException if a reader IO error occurs.
     */
    private KeypleMessageDto finalizeSeProcessing() {

      // Execute the action on the reader
      ((ObservableReader) reader).finalizeSeProcessing();

      return new KeypleMessageDto(msg).setBody(null);
    }
  }
}
