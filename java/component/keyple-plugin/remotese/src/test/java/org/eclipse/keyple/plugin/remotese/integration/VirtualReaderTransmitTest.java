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
package org.eclipse.keyple.plugin.remotese.integration;

import java.util.List;
import org.eclipse.keyple.core.card.message.CardRequest;
import org.eclipse.keyple.core.card.message.ChannelControl;
import org.eclipse.keyple.core.card.message.ProxyReader;
import org.eclipse.keyple.core.card.selection.MultiSelectionProcessing;
import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.core.service.exception.KeypleReaderException;
import org.eclipse.keyple.core.service.exception.KeypleReaderIOException;
import org.eclipse.keyple.plugin.remotese.pluginse.VirtualReader;
import org.eclipse.keyple.plugin.remotese.rm.json.SampleFactory;
import org.eclipse.keyple.plugin.stub.StubReader;
import org.eclipse.keyple.plugin.stub.StubReaderTest;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Test transmit scenarii extends configuration from VirtualReaderTest */
@Ignore
public class VirtualReaderTransmitTest extends VirtualReaderBaseTest {

  private static final Logger logger = LoggerFactory.getLogger(VirtualReaderTransmitTest.class);

  private VirtualReader virtualReader;
  private StubReader nativeReader;

  @Before
  public void setUp() throws Exception {
    Assert.assertEquals(0, SmartCardService.getInstance().getPlugins().size());

    initMasterNSlave();

    // configure and connect a Stub Native reader
    nativeReader = this.connectStubReader(NATIVE_READER_NAME, CLIENT_NODE_ID, true);

    // test virtual reader
    virtualReader = getVirtualReader();
  }

  @After
  public void tearDown() throws Exception {
    disconnectReader(NATIVE_READER_NAME);

    clearMasterNSlave();

    unregisterPlugins();

    Assert.assertEquals(0, SmartCardService.getInstance().getPlugins().size());
  }

  /*
   * TRANSMITS
   */

  @Ignore
  @Test
  public void testKOTransmitSet_NoSE() {

    try {
      StubReaderTest.genericSelectSe(virtualReader);

      ((ProxyReader) virtualReader)
          .transmitCardRequests(
              SampleFactory.getACardRequestList(),
              MultiSelectionProcessing.FIRST_MATCH,
              ChannelControl.KEEP_OPEN);
      // should throw KeypleReaderException
      Assert.assertTrue(false);

    } catch (KeypleReaderException e) {
      logger.info("KeypleReaderException was thrown as expected");
      // assert exception is thrown
      Assert.assertNotNull(e);
      // Assert.assertNotNull(e.getCardResponseSet());
      // Assert.assertNull(e.getCardResponse());
    }
  }

  @Ignore
  @Test
  public void testKOTransmit_NoSE() {

    try {
      StubReaderTest.genericSelectSe(((ProxyReader) virtualReader));

      ((ProxyReader) virtualReader)
          .transmitCardRequest(SampleFactory.getACardRequest(), ChannelControl.KEEP_OPEN);
      // should throw KeypleReaderException
      Assert.assertTrue(false);

    } catch (KeypleReaderException e) {
      logger.info("KeypleReaderException was thrown as expected");
      // assert exception is thrown
      Assert.assertNotNull(e);
      // Assert.assertNotNull(e.getCardResponseSet());
      // Assert.assertNull(e.getCardResponse());
      // should not be null but transmit is using transmitSet, this is the reason I guess
      // todo : VirtualReader transmit should not be using transmitSet
    }
  }

  /**
   * Successful Transmit with a Calypso command to a Calypso card
   *
   * @throws Exception
   */
  @Ignore // TODO update this test with the new API
  @Test
  public void rse_transmit_Hoplink_Sucessfull() throws Exception {
    int N_TIMES = 10;

    // insert card
    nativeReader.insertSe(StubReaderTest.hoplinkSE());

    Thread.sleep(1000);

    StubReaderTest.genericSelectSe(virtualReader);

    // test N_TIMES transmit with KEEP_OPEN
    for (int i = 0; i < N_TIMES; i++) {

      // test
      // TODO update this test with the new API
      // ReadRecordsCmdBuild poReadRecordCmd_T2Env =
      // new ReadRecordsCmdBuild(PoClass.ISO, (byte) 0x14,
      // ReadDataStructure.SINGLE_RECORD_DATA, (byte) 0x01, true, (byte) 0x20);
      // List<ApduRequest> poApduRequests=
      // Arrays.asList(poReadRecordCmd_T2Env.getApduRequest());
      // CardRequest cardRequest = new CardRequest(poApduRequestList);
      // Set<CardRequest> cardRequests = new LinkedHashSet<CardRequest>();
      // cardRequests.add(cardRequest);
      //
      // List<CardResponse> cardResponse = ((ProxyReader)
      // virtualReader).transmitSet(cardRequests);
      // // assert
      // Assert.assertTrue(cardResponse.get(0).getApduResponses().get(0).isSuccessful());
      //
      // logger.info("CardResponse returned as expected {}", cardResponse.get(0));
    }
  }

  @Test(expected = KeypleReaderException.class)
  public void rse_transmit_no_response() throws Exception {

    // insert card
    nativeReader.insertSe(StubReaderTest.noApduResponseSE());

    // wait for card to be detected
    Thread.sleep(500);

    // init Request
    List<CardRequest> requests = StubReaderTest.getNoResponseRequest();

    StubReaderTest.genericSelectSe(virtualReader);

    // test
    ((ProxyReader) virtualReader)
        .transmitCardRequests(
            requests, MultiSelectionProcessing.FIRST_MATCH, ChannelControl.KEEP_OPEN);
  }

  @Test
  public void transmit_partial_response_set_0() throws InterruptedException {

    // insert card
    nativeReader.insertSe(StubReaderTest.partialSE());

    // wait for card to be detected
    Thread.sleep(500);

    // init Request
    List<CardRequest> cardRequests = StubReaderTest.getPartialRequestList(0);

    try {
      StubReaderTest.genericSelectSe(virtualReader);

      ((ProxyReader) virtualReader)
          .transmitCardRequests(
              cardRequests, MultiSelectionProcessing.FIRST_MATCH, ChannelControl.KEEP_OPEN);

    } catch (KeypleReaderIOException ex) {
      logger.info(
          "KeypleReaderException was thrown as expected : {} {}",
          ex.getCardResponses(),
          ex.getCardResponse());

      Assert.assertEquals(ex.getCardResponses().size(), 1);
      Assert.assertEquals(ex.getCardResponses().get(0).getApduResponses().size(), 2);
    }
  }

  @Test
  public void transmit_partial_response_set_1() throws InterruptedException {

    // insert card
    nativeReader.insertSe(StubReaderTest.partialSE());

    // wait for card to be detected
    Thread.sleep(500);

    // init Request
    List<CardRequest> cardRequests = StubReaderTest.getPartialRequestList(1);

    try {
      StubReaderTest.genericSelectSe(virtualReader);

      ((ProxyReader) virtualReader)
          .transmitCardRequests(
              cardRequests, MultiSelectionProcessing.FIRST_MATCH, ChannelControl.KEEP_OPEN);

    } catch (KeypleReaderIOException ex) {
      logger.info(
          "KeypleReaderException was thrown as expected : {} {}",
          ex.getCardResponses(),
          ex.getCardResponse());
      Assert.assertEquals(ex.getCardResponses().size(), 2);
      Assert.assertEquals(ex.getCardResponses().get(0).getApduResponses().size(), 4);
      Assert.assertEquals(ex.getCardResponses().get(1).getApduResponses().size(), 2);
      Assert.assertEquals(ex.getCardResponses().get(1).getApduResponses().size(), 2);
    }
  }

  @Test
  public void transmit_partial_response_set_2() throws InterruptedException {

    // insert card
    nativeReader.insertSe(StubReaderTest.partialSE());

    // wait for card to be detected
    Thread.sleep(500);

    // init Request
    List<CardRequest> cardRequests = StubReaderTest.getPartialRequestList(2);

    // test
    try {
      StubReaderTest.genericSelectSe(virtualReader);

      ((ProxyReader) virtualReader)
          .transmitCardRequests(
              cardRequests, MultiSelectionProcessing.FIRST_MATCH, ChannelControl.KEEP_OPEN);

    } catch (KeypleReaderIOException ex) {
      logger.info(
          "KeypleReaderException was thrown as expected : {} {}",
          ex.getCardResponses(),
          ex.getCardResponse());
      Assert.assertEquals(ex.getCardResponses().size(), 3);
      Assert.assertEquals(ex.getCardResponses().get(0).getApduResponses().size(), 4);
      Assert.assertEquals(ex.getCardResponses().get(1).getApduResponses().size(), 4);
      Assert.assertEquals(ex.getCardResponses().get(2).getApduResponses().size(), 2);
    }
  }

  @Test
  public void transmit_partial_response_set_3() throws InterruptedException {

    // insert card
    nativeReader.insertSe(StubReaderTest.partialSE());

    // wait for card to be detected
    Thread.sleep(500);

    // init Request
    List<CardRequest> cardRequests = StubReaderTest.getPartialRequestList(3);

    // test
    try {
      StubReaderTest.genericSelectSe(virtualReader);

      ((ProxyReader) virtualReader)
          .transmitCardRequests(
              cardRequests, MultiSelectionProcessing.FIRST_MATCH, ChannelControl.KEEP_OPEN);

    } catch (KeypleReaderIOException ex) {
      logger.info(
          "KeypleReaderException was thrown as expected : {} {}",
          ex.getCardResponses(),
          ex.getCardResponse());
      Assert.assertEquals(ex.getCardResponses().size(), 3);
      Assert.assertEquals(ex.getCardResponses().get(0).getApduResponses().size(), 4);
      Assert.assertEquals(ex.getCardResponses().get(1).getApduResponses().size(), 4);
      Assert.assertEquals(ex.getCardResponses().get(2).getApduResponses().size(), 4);
    }
  }

  @Test
  public void transmit_partial_response_0() throws InterruptedException {

    // insert card
    nativeReader.insertSe(StubReaderTest.partialSE());

    // wait for card to be detected
    Thread.sleep(500);

    // init Request
    CardRequest cardRequest = StubReaderTest.getPartialRequest(0);

    // test
    try {
      StubReaderTest.genericSelectSe(virtualReader);

      ((ProxyReader) virtualReader).transmitCardRequest(cardRequest, ChannelControl.KEEP_OPEN);

    } catch (KeypleReaderIOException ex) {
      logger.info(
          "KeypleReaderException was thrown as expected : {} {}",
          ex.getCardResponses(),
          ex.getCardResponse());
      Assert.assertEquals(ex.getCardResponse().getApduResponses().size(), 0);
    }
  }

  @Test
  public void transmit_partial_response_1() throws InterruptedException {

    // insert card
    nativeReader.insertSe(StubReaderTest.partialSE());

    // wait for card to be detected
    Thread.sleep(500);

    // init Request
    CardRequest cardRequest = StubReaderTest.getPartialRequest(1);

    // test
    try {
      StubReaderTest.genericSelectSe(virtualReader);

      ((ProxyReader) virtualReader).transmitCardRequest(cardRequest, ChannelControl.KEEP_OPEN);

    } catch (KeypleReaderIOException ex) {
      logger.info(
          "KeypleReaderException was thrown as expected : {} {}",
          ex.getCardResponses(),
          ex.getCardResponse());
      Assert.assertEquals(ex.getCardResponse().getApduResponses().size(), 1);
    }
  }

  @Test
  public void transmit_partial_response_2() throws InterruptedException {

    // insert card
    nativeReader.insertSe(StubReaderTest.partialSE());

    // wait for card to be detected
    Thread.sleep(500);

    // init Request
    CardRequest cardRequest = StubReaderTest.getPartialRequest(2);

    // test
    try {
      StubReaderTest.genericSelectSe(virtualReader);

      ((ProxyReader) virtualReader).transmitCardRequest(cardRequest, ChannelControl.KEEP_OPEN);

    } catch (KeypleReaderIOException ex) {
      logger.info(
          "KeypleReaderException was thrown as expected : set : {}, cardResponse : {}",
          ex.getCardResponses(),
          ex.getCardResponse());
      Assert.assertEquals(ex.getCardResponse().getApduResponses().size(), 2);
    }
  }

  @Test
  public void transmit_partial_response_3() throws InterruptedException {

    // insert card
    nativeReader.insertSe(StubReaderTest.partialSE());

    // wait for card to be detected
    Thread.sleep(500);

    // init Request
    CardRequest cardRequest = StubReaderTest.getPartialRequest(3);

    try {
      // test
      StubReaderTest.genericSelectSe(virtualReader);

      ((ProxyReader) virtualReader).transmitCardRequest(cardRequest, ChannelControl.KEEP_OPEN);

    } catch (KeypleReaderIOException ex) {
      logger.info(
          "KeypleReaderException was thrown as expected : {} {}",
          ex.getCardResponses(),
          ex.getCardResponse());
      Assert.assertEquals(ex.getCardResponse().getApduResponses().size(), 3);
    }
  }
}
