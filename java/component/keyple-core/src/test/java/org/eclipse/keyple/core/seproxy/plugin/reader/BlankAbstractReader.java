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
package org.eclipse.keyple.core.seproxy.plugin.reader;

import java.util.List;
import org.eclipse.keyple.core.seproxy.MultiSelectionProcessing;
import org.eclipse.keyple.core.seproxy.message.CardRequest;
import org.eclipse.keyple.core.seproxy.message.CardResponse;
import org.eclipse.keyple.core.seproxy.message.ChannelControl;

/** A blank class extending AbstractReader only purpose is to be tested and spied by mockito */
public class BlankAbstractReader extends AbstractReader {

  public BlankAbstractReader(String pluginName, String readerName) {
    super(pluginName, readerName);
  }

  @Override
  protected List<CardResponse> processCardRequests(
      List<CardRequest> cardRequests,
      MultiSelectionProcessing multiSelectionProcessing,
      ChannelControl channelControl) {
    return null;
  }

  @Override
  protected CardResponse processCardRequest(
      CardRequest cardRequest, ChannelControl channelControl) {
    return null;
  }

  @Override
  public boolean isCardPresent() {
    return false;
  }

  @Override
  public void activateProtocol(String readerProtocolName, String applicationProtocolName) {}

  @Override
  public void deactivateProtocol(String cardProtocol) {}

  @Override
  public boolean isContactless() {
    return true;
  }

  @Override
  public void releaseChannel() {}
}
