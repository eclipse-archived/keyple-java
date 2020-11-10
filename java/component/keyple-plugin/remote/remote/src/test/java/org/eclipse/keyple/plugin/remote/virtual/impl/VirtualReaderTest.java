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
package org.eclipse.keyple.plugin.remote.virtual.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.google.gson.reflect.TypeToken;
import java.util.*;
import org.eclipse.keyple.core.card.message.*;
import org.eclipse.keyple.core.card.selection.MultiSelectionProcessing;
import org.eclipse.keyple.core.service.exception.KeypleReaderIOException;
import org.eclipse.keyple.core.util.json.BodyError;
import org.eclipse.keyple.core.util.json.KeypleJsonParser;
import org.eclipse.keyple.plugin.remote.core.KeypleMessageDto;
import org.eclipse.keyple.plugin.remote.core.exception.KeypleTimeoutException;
import org.eclipse.keyple.plugin.remote.core.impl.AbstractKeypleNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class VirtualReaderTest {

  static final String pluginName = "pluginName";
  static final String nativeReaderName = "nativeReaderName";

  VirtualReader reader;
  AbstractKeypleNode node;

  @Before
  public void setUp() {
    node = mock(AbstractKeypleNode.class);
    reader = new VirtualReader(pluginName, nativeReaderName, node, "val1", null);
  }

  @Test
  public void constructor_shouldGenerateName() {
    assertThat(reader.getName()).isNotEmpty();
  }

  @Test
  public void processCardRequest_whenOk_shouldCallTheHandlerAndReturnResponses() {

    // init request
    CardRequest cardRequest = SampleFactory.getACardRequest();
    ChannelControl channelControl = ChannelControl.KEEP_OPEN;

    // init response
    CardResponse cardResponse = SampleFactory.getACardResponse();

    KeypleMessageDto responseDto =
        new KeypleMessageDto() //
            .setAction(KeypleMessageDto.Action.TRANSMIT.name()) //
            .setVirtualReaderName(reader.getName()) //
            .setNativeReaderName(reader.nativeReaderName) //
            .setBody(KeypleJsonParser.getParser().toJson(cardResponse, CardResponse.class));

    doReturn(responseDto).when(node).sendRequest(any(KeypleMessageDto.class));

    // execute
    CardResponse cardResponseReturned = reader.processCardRequest(cardRequest, channelControl);

    // verify
    assertThat(cardResponseReturned).isEqualToComparingFieldByField(cardResponse);
  }

  @Test(expected = KeypleTimeoutException.class)
  public void processCardRequest_whenNodeTimeout_shouldThrowKTE() {

    // init response
    mockTimeout();

    // init request
    CardRequest cardRequest = SampleFactory.getACardRequest();
    ChannelControl channelControl = ChannelControl.KEEP_OPEN;

    // execute
    reader.processCardRequest(cardRequest, channelControl);
  }

  @Test(expected = KeypleReaderIOException.class)
  public void processCardRequest_whenError_shouldThrowOriginalException() {

    // init response
    mockError();

    // init request
    CardRequest cardRequest = SampleFactory.getACardRequest();
    ChannelControl channelControl = ChannelControl.KEEP_OPEN;

    // execute
    reader.processCardRequest(cardRequest, channelControl);
  }

  @Test
  public void processCardRequests_whenOk_shouldCallTheHandlerAndReturnResponses() {

    // init request
    List<CardSelectionRequest> cardSelectionRequests =
        SampleFactory.getACardRequestList_ISO14443_4();
    MultiSelectionProcessing multiCardRequestProcessing = MultiSelectionProcessing.FIRST_MATCH;
    ChannelControl channelControl = ChannelControl.KEEP_OPEN;

    // init response
    List<CardSelectionResponse> cardResponses = SampleFactory.getCompleteResponseList();

    KeypleMessageDto responseDto =
        new KeypleMessageDto() //
            .setAction(KeypleMessageDto.Action.TRANSMIT_CARD_SELECTION.name()) //
            .setVirtualReaderName(reader.getName()) //
            .setNativeReaderName(reader.nativeReaderName) //
            .setBody(
                KeypleJsonParser.getParser()
                    .toJson(cardResponses, new TypeToken<ArrayList<CardResponse>>() {}.getType()));

    doReturn(responseDto).when(node).sendRequest(any(KeypleMessageDto.class));

    // execute
    List<CardSelectionResponse> cardResponsesReturned =
        reader.processCardSelectionRequests(
            cardSelectionRequests, multiCardRequestProcessing, channelControl);

    // verify
    assertThat(cardResponsesReturned).hasSameElementsAs(cardResponses);
  }

  @Test(expected = KeypleTimeoutException.class)
  public void processCardRequests_whenNodeTimeout_shouldThrowKTE() {

    // init response
    mockTimeout();

    // init request
    List<CardSelectionRequest> cardSelectionRequests =
        SampleFactory.getACardRequestList_ISO14443_4();
    MultiSelectionProcessing multiCardRequestProcessing = MultiSelectionProcessing.FIRST_MATCH;
    ChannelControl channelControl = ChannelControl.KEEP_OPEN;

    // execute
    reader.processCardSelectionRequests(
        cardSelectionRequests, multiCardRequestProcessing, channelControl);
  }

  @Test(expected = KeypleReaderIOException.class)
  public void processCardRequests_whenError_shouldThrowOriginalException() {

    // init response
    mockError();

    // init request
    List<CardSelectionRequest> cardSelectionRequests =
        SampleFactory.getACardRequestList_ISO14443_4();
    MultiSelectionProcessing multiCardRequestProcessing = MultiSelectionProcessing.FIRST_MATCH;
    ChannelControl channelControl = ChannelControl.KEEP_OPEN;

    // execute
    reader.processCardSelectionRequests(
        cardSelectionRequests, multiCardRequestProcessing, channelControl);
  }

  @Test
  public void isSePresent_whenOk_shouldCallTheHandlerAndReturnResponses() {

    // init
    KeypleMessageDto responseDto =
        new KeypleMessageDto() //
            .setAction(KeypleMessageDto.Action.IS_CARD_PRESENT.name()) //
            .setVirtualReaderName(reader.getName()) //
            .setNativeReaderName(reader.nativeReaderName) //
            .setBody(KeypleJsonParser.getParser().toJson(true, Boolean.class));

    doReturn(responseDto).when(node).sendRequest(any(KeypleMessageDto.class));

    // execute
    boolean result = reader.isCardPresent();

    // verify
    assertThat(result).isTrue();
  }

  @Test(expected = KeypleTimeoutException.class)
  public void isSePresent_whenNodeTimeout_shouldThrowKTE() {
    // init
    mockTimeout();
    // execute
    reader.isCardPresent();
  }

  @Test(expected = KeypleReaderIOException.class)
  public void isSePresent_whenError_shouldThrowOriginalException() {
    // init
    mockError();
    // execute
    reader.isCardPresent();
  }

  @Test
  public void isReaderContactless_whenOk_shouldCallTheHandlerAndReturnResponses() {

    // init
    KeypleMessageDto responseDto =
        new KeypleMessageDto() //
            .setAction(KeypleMessageDto.Action.IS_READER_CONTACTLESS.name()) //
            .setVirtualReaderName(reader.getName()) //
            .setNativeReaderName(reader.nativeReaderName) //
            .setBody(KeypleJsonParser.getParser().toJson(true, Boolean.class));

    doReturn(responseDto).when(node).sendRequest(any(KeypleMessageDto.class));

    // execute
    boolean result = reader.isContactless();

    // verify
    assertThat(result).isTrue();
  }

  @Test(expected = KeypleTimeoutException.class)
  public void isReaderContactless_whenNodeTimeout_shouldThrowKTE() {
    // init
    mockTimeout();
    // execute
    reader.isContactless();
  }

  @Test(expected = KeypleReaderIOException.class)
  public void isReaderContactless_whenError_shouldThrowOriginalException() {
    // init
    mockError();
    // execute
    reader.isContactless();
  }

  @Test
  public void releaseChannel_whenOk_shouldCallTheHandlerAndReturnResponses() {

    // init response
    KeypleMessageDto responseDto =
        new KeypleMessageDto() //
            .setAction(KeypleMessageDto.Action.RELEASE_CHANNEL.name()) //
            .setVirtualReaderName(reader.getName()) //
            .setNativeReaderName(reader.nativeReaderName);

    doReturn(responseDto).when(node).sendRequest(any(KeypleMessageDto.class));

    // execute
    reader.releaseChannel();
  }

  @Test(expected = KeypleTimeoutException.class)
  public void releaseChannel_whenNodeTimeout_shouldThrowKTE() {
    // init
    mockTimeout();
    // execute
    reader.releaseChannel();
  }

  @Test(expected = KeypleReaderIOException.class)
  public void releaseChannel_whenError_shouldThrowOriginalException() {
    // init
    mockError();
    // execute
    reader.releaseChannel();
  }

  @Test
  public void getSessionId_whenIsSet_shouldReturnCurrentValue() {
    String sessionId = reader.getSessionId();
    assertThat(sessionId).isEqualTo("val1");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void activateProtocol__shouldThrowUOE() {
    reader.activateProtocol("any", "any");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void deactivateProtocol__shouldThrowUOE() {
    reader.deactivateProtocol("any");
  }

  private void mockTimeout() {
    doThrow(new KeypleTimeoutException("test")).when(node).sendRequest(any(KeypleMessageDto.class));
  }

  private void mockError() {

    KeypleReaderIOException error = SampleFactory.getASimpleKeypleException();

    KeypleMessageDto responseDto =
        new KeypleMessageDto() //
            .setAction(KeypleMessageDto.Action.ERROR.name()) //
            .setVirtualReaderName(reader.getName()) //
            .setNativeReaderName(reader.nativeReaderName) //
            .setBody(KeypleJsonParser.getParser().toJson(new BodyError(error)));

    doReturn(responseDto).when(node).sendRequest(any(KeypleMessageDto.class));
  }
}
