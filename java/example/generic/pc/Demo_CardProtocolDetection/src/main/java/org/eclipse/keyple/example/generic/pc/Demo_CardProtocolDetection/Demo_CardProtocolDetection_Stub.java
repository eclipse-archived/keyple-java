/* **************************************************************************************
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.eclipse.keyple.example.generic.pc.Demo_CardProtocolDetection;

import org.eclipse.keyple.core.service.Plugin;
import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.core.service.exception.KeyplePluginInstantiationException;
import org.eclipse.keyple.core.service.exception.KeyplePluginNotFoundException;
import org.eclipse.keyple.core.service.exception.KeypleReaderNotFoundException;
import org.eclipse.keyple.core.service.util.ContactlessCardCommonProtocols;
import org.eclipse.keyple.example.common.generic.stub.StubMifareClassic;
import org.eclipse.keyple.example.common.generic.stub.StubMifareDesfire;
import org.eclipse.keyple.example.common.generic.stub.StubMifareUL;
import org.eclipse.keyple.plugin.stub.StubPlugin;
import org.eclipse.keyple.plugin.stub.StubPluginFactory;
import org.eclipse.keyple.plugin.stub.StubReader;
import org.eclipse.keyple.plugin.stub.StubSupportedProtocols;

/** This class handles the reader events generated by the SmartCardService */
public class Demo_CardProtocolDetection_Stub {

  public Demo_CardProtocolDetection_Stub() {
    super();
  }

  /**
   * Application entry
   *
   * @param args the program arguments
   * @throws IllegalArgumentException in case of a bad argument
   * @throws InterruptedException if thread error occurs
   */
  public static void main(String[] args)
      throws InterruptedException, KeyplePluginNotFoundException,
          KeyplePluginInstantiationException {
    // get the SmartCardService instance
    SmartCardService smartCardService = SmartCardService.getInstance();

    final String STUB_PLUGIN_NAME = "stub1";

    // Register Stub plugin in the platform
    Plugin stubPlugin =
        smartCardService.registerPlugin(new StubPluginFactory(STUB_PLUGIN_NAME, null, null));

    // create an observer class to handle the card operations
    CardProtocolDetectionEngine observer = new CardProtocolDetectionEngine();

    // Plug PO reader.
    ((StubPlugin) stubPlugin).plugStubReader("poReader", true);

    Thread.sleep(200);

    StubReader poReader = null;
    try {
      poReader = (StubReader) (stubPlugin.getReader("poReader"));
    } catch (KeypleReaderNotFoundException e) {
      e.printStackTrace();
    }

    observer.setReader(poReader);

    /* Activate protocols */
    poReader.activateProtocol(
        StubSupportedProtocols.ISO_14443_4.name(),
        ContactlessCardCommonProtocols.ISO_14443_4.name());
    poReader.activateProtocol(StubSupportedProtocols.MIFARE_CLASSIC.name(), "MIFARE_CLASSIC");
    poReader.activateProtocol(StubSupportedProtocols.MEMORY_ST25.name(), "MEMORY_ST25");

    // Set terminal as Observer of the first reader
    poReader.addObserver(observer);

    Thread.sleep(300);

    poReader.removeCard();

    Thread.sleep(100);

    poReader.insertCard(new StubMifareClassic());

    Thread.sleep(300);

    poReader.removeCard();

    Thread.sleep(100);

    // insert Mifare UltraLight
    poReader.insertCard(new StubMifareUL());

    Thread.sleep(300);

    poReader.removeCard();

    Thread.sleep(100);

    // insert Mifare Desfire
    poReader.insertCard(new StubMifareDesfire());

    Thread.sleep(300);

    poReader.removeCard();

    Thread.sleep(100);

    System.exit(0);
  }
}
