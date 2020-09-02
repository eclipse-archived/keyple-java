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
package org.eclipse.keyple.plugin.remotese.virtualse.impl;

import java.util.List;
import java.util.Map;
import org.eclipse.keyple.core.selection.AbstractMatchingSe;
import org.eclipse.keyple.core.seproxy.MultiSeRequestProcessing;
import org.eclipse.keyple.core.seproxy.message.ChannelControl;
import org.eclipse.keyple.core.seproxy.message.ProxyReader;
import org.eclipse.keyple.core.seproxy.message.SeRequest;
import org.eclipse.keyple.core.seproxy.message.SeResponse;
import org.eclipse.keyple.core.seproxy.protocol.SeProtocol;
import org.eclipse.keyple.core.seproxy.protocol.TransmissionMode;
import org.eclipse.keyple.core.util.Assert;
import org.eclipse.keyple.core.util.json.KeypleJsonParser;
import org.eclipse.keyple.plugin.remotese.virtualse.RemoteSeServerReader;

/**
 * (package-private)<br>
 * Abstract Server Virtual Reader class.<br>
 * This class is a decorator of a {@link AbstractVirtualReader}.
 */
abstract class AbstractServerVirtualReader implements RemoteSeServerReader, ProxyReader {

  private final AbstractVirtualReader reader;
  private final String serviceId;
  private final String initialSeContentJson;
  private final String userInputDataJson;

  /**
   * (package-private)<br>
   * Constructor
   *
   * @param reader The reader to decorate (must be not null).
   * @param serviceId The service id (must be not null).
   * @param userInputDataJson The user input data as a JSON string (optional).
   * @param initialSeContentJson The initial SE content as a JSON string (optional).
   */
  AbstractServerVirtualReader(
      AbstractVirtualReader reader,
      String serviceId,
      String userInputDataJson,
      String initialSeContentJson) {
    this.reader = reader;
    this.serviceId = serviceId;
    this.userInputDataJson = userInputDataJson;
    this.initialSeContentJson = initialSeContentJson;
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  public List<SeResponse> transmitSeRequests(
      List<SeRequest> seRequests,
      MultiSeRequestProcessing multiSeRequestProcessing,
      ChannelControl channelControl) {
    return reader.transmitSeRequests(seRequests, multiSeRequestProcessing, channelControl);
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  public SeResponse transmitSeRequest(SeRequest seRequest, ChannelControl channelControl) {
    return reader.transmitSeRequest(seRequest, channelControl);
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  public String getServiceId() {
    return serviceId;
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  public <T> T getUserInputData(Class<T> classOfT) {
    Assert.getInstance().notNull(classOfT, "classOfT");
    return userInputDataJson != null
        ? KeypleJsonParser.getParser().fromJson(userInputDataJson, classOfT)
        : null;
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  public <T extends AbstractMatchingSe> T getInitialSeContent(Class<T> classOfT) {
    Assert.getInstance().notNull(classOfT, "classOfT");
    return initialSeContentJson != null
        ? KeypleJsonParser.getParser().fromJson(initialSeContentJson, classOfT)
        : null;
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  public boolean isSePresent() {
    return reader.isSePresent();
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  public void addSeProtocolSetting(SeProtocol seProtocol, String protocolRule) {
    reader.addSeProtocolSetting(seProtocol, protocolRule);
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  public void setSeProtocolSetting(Map<SeProtocol, String> protocolSetting) {
    reader.setSeProtocolSetting(protocolSetting);
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  public TransmissionMode getTransmissionMode() {
    return reader.getTransmissionMode();
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  public String getName() {
    return reader.getName();
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  public Map<String, String> getParameters() {
    return reader.getParameters();
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  public void setParameter(String key, String value) {
    reader.setParameter(key, value);
  }

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Override
  public void setParameters(Map<String, String> parameters) {
    reader.setParameters(parameters);
  }
}
