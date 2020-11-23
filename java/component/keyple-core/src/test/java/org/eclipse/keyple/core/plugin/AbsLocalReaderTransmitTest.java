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
package org.eclipse.keyple.core.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.eclipse.keyple.core.plugin.AbsLocalReaderSelectionTest.ATR;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.keyple.core.CoreBaseTest;
import org.eclipse.keyple.core.card.message.ApduRequest;
import org.eclipse.keyple.core.card.message.CardRequest;
import org.eclipse.keyple.core.card.message.CardResponse;
import org.eclipse.keyple.core.card.message.CardSelectionRequest;
import org.eclipse.keyple.core.card.message.CardSelectionResponse;
import org.eclipse.keyple.core.card.message.ChannelControl;
import org.eclipse.keyple.core.card.selection.CardSelector;
import org.eclipse.keyple.core.card.selection.MultiSelectionProcessing;
import org.eclipse.keyple.core.service.exception.KeypleReaderException;
import org.eclipse.keyple.core.service.exception.KeypleReaderIOException;
import org.eclipse.keyple.core.service.util.ContactlessCardCommonProtocols;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Process card Request Test for AbstractLocalReader */
public class AbsLocalReaderTransmitTest extends CoreBaseTest {

  private static final Logger logger = LoggerFactory.getLogger(AbsLocalReaderTransmitTest.class);

  static final String PLUGIN_NAME = "AbsLocalReaderTransmitTestP";
  static final String READER_NAME = "AbsLocalReaderTransmitTest";

  static final byte[] RESP_SUCCESS = ByteArrayUtil.fromHex("90 00");
  static final byte[] RESP_FAIL = ByteArrayUtil.fromHex("00 00");

  static final byte[] APDU_SUCCESS = ByteArrayUtil.fromHex("00 01");
  static final byte[] APDU_FAIL = ByteArrayUtil.fromHex("00 02");
  static final byte[] APDU_IOEXC = ByteArrayUtil.fromHex("00 03");

  @Before
  public void setUp() {
    logger.info("------------------------------");
    logger.info("Test {}", name.getMethodName() + "");
    logger.info("------------------------------");
  }

  @Test
  public void transmit_partial_response_set_0() throws Exception {
    AbstractLocalReader reader = getSpy(PLUGIN_NAME, READER_NAME);

    // init Request
    List<CardSelectionRequest> cardSelectionRequests = getPartialRequestList(reader, 0);
    try {
      // test
      reader.processCardSelectionRequests(
          cardSelectionRequests, MultiSelectionProcessing.PROCESS_ALL, ChannelControl.CLOSE_AFTER);
      fail("A KeypleReaderIOException was expected");
    } catch (KeypleReaderIOException ex) {
      assertThat(ex.getCardSelectionResponses().size()).isEqualTo(1);
      assertThat(ex.getCardSelectionResponses().get(0).getCardResponse().getApduResponses().size())
          .isEqualTo(2);
    }
  }

  @Test
  public void transmit_partial_response_set_1() throws Exception {
    AbstractLocalReader reader = getSpy(PLUGIN_NAME, READER_NAME);

    List<CardSelectionRequest> cardSelectionRequests = getPartialRequestList(reader, 1);
    try {
      // test
      reader.processCardSelectionRequests(
          cardSelectionRequests, MultiSelectionProcessing.PROCESS_ALL, ChannelControl.CLOSE_AFTER);
      fail("A KeypleReaderIOException was expected");
    } catch (KeypleReaderIOException ex) {
      assertThat(ex.getCardSelectionResponses().size()).isEqualTo(2);
      assertThat(ex.getCardSelectionResponses().get(0).getCardResponse().getApduResponses().size())
          .isEqualTo(4);
      assertThat(ex.getCardSelectionResponses().get(1).getCardResponse().getApduResponses().size())
          .isEqualTo(2);
    }
  }

  @Test
  public void transmit_partial_response_set_2() throws Exception {
    AbstractLocalReader reader = getSpy(PLUGIN_NAME, READER_NAME);

    List<CardSelectionRequest> cardSelectionRequests = getPartialRequestList(reader, 2);
    try {
      // test
      reader.processCardSelectionRequests(
          cardSelectionRequests, MultiSelectionProcessing.PROCESS_ALL, ChannelControl.CLOSE_AFTER);
      fail("A KeypleReaderIOException was expected");
    } catch (KeypleReaderIOException ex) {
      assertThat(ex.getCardSelectionResponses().size()).isEqualTo(3);
      assertThat(ex.getCardSelectionResponses().get(0).getCardResponse().getApduResponses().size())
          .isEqualTo(4);
      assertThat(ex.getCardSelectionResponses().get(1).getCardResponse().getApduResponses().size())
          .isEqualTo(4);
      assertThat(ex.getCardSelectionResponses().get(2).getCardResponse().getApduResponses().size())
          .isEqualTo(2);
    }
  }

  @Test
  public void transmit_partial_response_set_3() throws Exception {
    AbstractLocalReader reader = getSpy(PLUGIN_NAME, READER_NAME);

    List<CardSelectionRequest> cardSelectionRequests = getPartialRequestList(reader, 3);
    try {
      // test
      List<CardSelectionResponse> cardSelectionResponses =
          reader.processCardSelectionRequests(
              cardSelectionRequests,
              MultiSelectionProcessing.PROCESS_ALL,
              ChannelControl.CLOSE_AFTER);
      assertThat(cardSelectionResponses.size()).isEqualTo(3);
      assertThat(cardSelectionResponses.get(0).getCardResponse().getApduResponses().size())
          .isEqualTo(4);
      assertThat(cardSelectionResponses.get(1).getCardResponse().getApduResponses().size())
          .isEqualTo(4);
      assertThat(cardSelectionResponses.get(2).getCardResponse().getApduResponses().size())
          .isEqualTo(0);

    } catch (KeypleReaderException ex) {
      fail("Should not throw exception");
    }
  }

  @Test
  public void transmit_first_match() throws Exception {
    AbstractLocalReader reader = getSpy(PLUGIN_NAME, READER_NAME);

    List<CardSelectionRequest> cardSelectionRequests = getPartialRequestList(reader, 3);
    try {
      // test
      List<CardSelectionResponse> cardSelectionResponses =
          reader.processCardSelectionRequests(
              cardSelectionRequests,
              MultiSelectionProcessing.FIRST_MATCH,
              ChannelControl.CLOSE_AFTER);
      assertThat(cardSelectionResponses.size()).isEqualTo(1);
      assertThat(cardSelectionResponses.get(0).getCardResponse().getApduResponses().size())
          .isEqualTo(4);
    } catch (KeypleReaderException ex) {
      fail("Should not throw exception");
    }
  }

  @Test
  public void transmit_partial_response_0() throws Exception {
    AbstractLocalReader reader = getSpy(PLUGIN_NAME, READER_NAME);

    CardRequest cardRequest = getPartialRequest(reader, 0).getCardRequest();
    try {
      // test
      reader.processCardRequest(cardRequest, ChannelControl.KEEP_OPEN);
    } catch (KeypleReaderIOException ex) {
      logger.error("", ex);
      assertThat(ex.getCardResponse().getApduResponses().size()).isEqualTo(0);
    }
  }

  @Test
  public void transmit_partial_response_1() throws Exception {
    AbstractLocalReader reader = getSpy(PLUGIN_NAME, READER_NAME);

    CardRequest cardRequest = getPartialRequest(reader, 1).getCardRequest();
    try {
      // test
      reader.processCardRequest(cardRequest, ChannelControl.CLOSE_AFTER);
      fail("Should throw exception");

    } catch (KeypleReaderIOException ex) {
      assertThat(ex.getCardResponse().getApduResponses().size()).isEqualTo(1);
    }
  }

  @Test
  public void transmit_partial_response_2() throws Exception {
    AbstractLocalReader reader = getSpy(PLUGIN_NAME, READER_NAME);

    CardRequest cardRequest = getPartialRequest(reader, 2).getCardRequest();
    try {
      // test
      reader.processCardRequest(cardRequest, ChannelControl.CLOSE_AFTER);
      fail("Should throw exception");

    } catch (KeypleReaderIOException ex) {
      assertThat(ex.getCardResponse().getApduResponses().size()).isEqualTo(2);
    }
  }

  @Test
  public void transmit_partial_response_3() throws Exception {
    AbstractLocalReader reader = getSpy(PLUGIN_NAME, READER_NAME);

    CardRequest cardRequest = getPartialRequest(reader, 3).getCardRequest();
    try {
      // test
      CardResponse cardResponse =
          reader.processCardRequest(cardRequest, ChannelControl.CLOSE_AFTER);
      assertThat(cardResponse.getApduResponses().size()).isEqualTo(3);
    } catch (KeypleReaderException ex) {
      fail("Should not throw exception");
    }
  }

  /*
   * Partial response set: multiple read records commands, one is not defined in the StubSE
   *
   * An Exception will be thrown.
   */
  public static List<CardSelectionRequest> getPartialRequestList(
      AbstractLocalReader r, int scenario) {

    CardSelector.AtrFilter atrFilter = new CardSelector.AtrFilter(ATR);
    CardSelector selector =
        CardSelector.builder()
            .cardProtocol(ContactlessCardCommonProtocols.ISO_14443_4.name())
            .atrFilter(atrFilter)
            .build();

    CardSelector failSelector =
        CardSelector.builder().cardProtocol("MIFARE_ULTRA_LIGHT").atrFilter(atrFilter).build();

    ApduRequest apduOK = new ApduRequest(APDU_SUCCESS, false);
    ApduRequest apduKO = new ApduRequest(APDU_IOEXC, false);

    List<ApduRequest> poApduRequests1 = new ArrayList<ApduRequest>();
    poApduRequests1.add(apduOK);
    poApduRequests1.add(apduOK);
    poApduRequests1.add(apduOK);
    poApduRequests1.add(apduOK);

    List<ApduRequest> poApduRequests2 = new ArrayList<ApduRequest>();
    poApduRequests2.add(apduOK);
    poApduRequests2.add(apduOK);
    poApduRequests2.add(apduOK);
    poApduRequests2.add(apduOK);

    List<ApduRequest> poApduRequests3 = new ArrayList<ApduRequest>();
    poApduRequests3.add(apduOK);
    poApduRequests3.add(apduOK);
    poApduRequests3.add(apduKO);
    poApduRequests3.add(apduOK);

    CardSelectionRequest cardRequest1 =
        new CardSelectionRequest(selector, new CardRequest(poApduRequests1));
    CardSelectionRequest cardRequest2 =
        new CardSelectionRequest(selector, new CardRequest(poApduRequests2));

    CardSelectionRequest cardRequest4 =
        new CardSelectionRequest(failSelector, new CardRequest(poApduRequests1));

    /* This CardRequest fails at step 3 */
    CardSelectionRequest cardRequest3 =
        new CardSelectionRequest(selector, new CardRequest(poApduRequests3));

    List<CardSelectionRequest> cardSelectionRequests = new ArrayList<CardSelectionRequest>();

    switch (scenario) {
      case 0:
        /* 0 response Set */
        cardSelectionRequests.add(cardRequest3); // fails
        cardSelectionRequests.add(cardRequest1); // succeeds
        cardSelectionRequests.add(cardRequest2); // succeeds
        break;
      case 1:
        /* 1 response Set */
        cardSelectionRequests.add(cardRequest1); // succeeds
        cardSelectionRequests.add(cardRequest3); // fails
        cardSelectionRequests.add(cardRequest2); // succeeds
        break;
      case 2:
        /* 2 responses Set */
        cardSelectionRequests.add(cardRequest1); // succeeds
        cardSelectionRequests.add(cardRequest2); // succeeds
        cardSelectionRequests.add(cardRequest3); // fails
        break;
      case 3:
        /* 3 responses Set */
        cardSelectionRequests.add(cardRequest1); // succeeds
        cardSelectionRequests.add(cardRequest2); // succeeds
        cardSelectionRequests.add(cardRequest4); // selection fails
        break;
      case 4:
        /* 3 responses Set */
        cardSelectionRequests.add(cardRequest1); // succeeds
        break;
      default:
    }

    return cardSelectionRequests;
  }

  public static CardSelectionRequest getPartialRequest(AbstractLocalReader r, int scenario) {

    /*
     * CardSelector.AtrFilter atrFilter = new CardSelector.AtrFilter(ATR); CardSelector selector = new
     * CardSelector( ContactlessCardCommonProtocols.ISO_14443_4, atrFilter, null, "iso");
     *
     */

    CardSelector aidSelector = AbsLocalReaderSelectionTest.getAidSelector();

    ApduRequest apduOK = new ApduRequest(APDU_SUCCESS, false);
    ApduRequest apduKO = new ApduRequest(APDU_IOEXC, false);

    List<ApduRequest> poApduRequests = new ArrayList<ApduRequest>();

    switch (scenario) {
      case 0:
        poApduRequests.add(apduKO); // fails
        poApduRequests.add(apduOK); // succeeds
        poApduRequests.add(apduOK); // succeeds
        break;
      case 1:
        poApduRequests.add(apduOK); // succeeds
        poApduRequests.add(apduKO); // fails
        poApduRequests.add(apduOK); // succeeds
        break;
      case 2:
        poApduRequests.add(apduOK); // succeeds
        poApduRequests.add(apduOK); // succeeds
        poApduRequests.add(apduKO); // fails
        break;
      case 3:
        poApduRequests.add(apduOK); // succeeds
        poApduRequests.add(apduOK); // succeeds
        poApduRequests.add(apduOK); // succeeds
        break;
      default:
        break;
    }

    return new CardSelectionRequest(aidSelector, new CardRequest(poApduRequests));
  }
  /*
   * Partial response: multiple read records commands, one is not defined in the StubSE
   *
   * An Exception will be thrown.
   */

  /**
   * Return a basic spy reader that responds to apdu
   *
   * @param pluginName
   * @param readerName
   * @return basic spy reader
   * @throws KeypleReaderException
   */
  public static AbstractLocalReader getSpy(String pluginName, String readerName) {
    AbstractLocalReader r = Mockito.spy(new BlankAbstractLocalReader(pluginName, readerName));

    configure(r);

    return r;
  }

  public static void configure(AbstractLocalReader r) {

    r.register();

    // activate and accept ISO_14443_4
    r.activateProtocol(
        ContactlessCardCommonProtocols.ISO_14443_4.name(),
        ContactlessCardCommonProtocols.ISO_14443_4.name());

    when(r.isCurrentProtocol(ContactlessCardCommonProtocols.ISO_14443_4.name())).thenReturn(true);

    // return art
    when(r.getATR()).thenReturn(ByteArrayUtil.fromHex(ATR));

    // success apdu
    doReturn(RESP_SUCCESS).when(r).transmitApdu(APDU_SUCCESS);

    // fail apdu
    doReturn(RESP_FAIL).when(r).transmitApdu(APDU_FAIL);

    // io exception apdu
    doThrow(new KeypleReaderIOException("io exception at transmitting " + APDU_IOEXC))
        .when(r)
        .transmitApdu(APDU_IOEXC);

    // aid selection
    doReturn(
            ByteArrayUtil.fromHex(
                "6F25840BA000000291A00000019102A516BF0C13C70800000000C0E11FA653070A3C230C1410019000"))
        .when(r)
        .transmitApdu(ByteArrayUtil.fromHex("00 A4 04 00 0A A0 00 00 02 91 A0 00 00 01 91 00"));
  }
}
