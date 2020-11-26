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
package org.eclipse.keyple.plugin.remotese.pluginse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import org.eclipse.keyple.core.seproxy.MultiSelectionProcessing;
import org.eclipse.keyple.core.seproxy.Reader;
import org.eclipse.keyple.core.seproxy.SeProxyService;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException;
import org.eclipse.keyple.core.seproxy.message.CardRequest;
import org.eclipse.keyple.core.seproxy.message.CardResponse;
import org.eclipse.keyple.core.seproxy.message.ChannelControl;
import org.eclipse.keyple.core.seproxy.message.ProxyReader;
import org.eclipse.keyple.plugin.remotese.integration.Integration;
import org.eclipse.keyple.plugin.remotese.integration.VirtualReaderBaseTest;
import org.eclipse.keyple.plugin.remotese.rm.json.SampleFactory;
import org.eclipse.keyple.plugin.stub.StubReaderTest;
import org.junit.*;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Unit Test Virtual Reader -> Native Reader */
public class VirtualReaderTest extends VirtualReaderBaseTest {

  private static final Logger logger = LoggerFactory.getLogger(VirtualReaderTest.class);

  final String NATIVE_READER_NAME = "testStubReader";
  final String CLIENT_NODE_ID = "testClientNodeId";
  final String SERVER_NODE_ID = "testServerNodeId";

  final long RPC_TIMEOUT = 1000;

  private VirtualReader virtualReader;
  private ProxyReader nativeReader;

  String RSE_PLUGIN = "TimeoutTest";

  @Before
  public void setUp() throws Exception {
    Assert.assertEquals(0, SeProxyService.getInstance().getPlugins().size());

    initMasterNSlave();

    // configure and connect a Mock Reader
    nativeReader = connectMockReader("mockReader");

    // get the paired virtual reader
    virtualReader = getVirtualReader();
  }

  @After
  public void tearDown() throws Exception {
    disconnectReader(NATIVE_READER_NAME);
    clearMasterNSlave();
    unregisterPlugins();
    // clean plugin
    seProxyService.unregisterPlugin(RSE_PLUGIN);
    Assert.assertEquals(0, SeProxyService.getInstance().getPlugins().size());
  }

  /**
   * Invoke a transmitSeRequests on a failing DtoNode, no Dto will be received, timeout should be
   * thrown
   *
   * @throws Exception
   */
  @Test(expected = KeypleReaderException.class)
  public void transmit_timeout() throws Exception {

    // create a specific MasterAPI with a fake DtoNode
    MasterAPI masterAPI =
        new MasterAPI(
            SeProxyService.getInstance(),
            Integration.getFakeDtoNode(),
            RPC_TIMEOUT,
            MasterAPI.PLUGIN_TYPE_DEFAULT,
            RSE_PLUGIN);

    // get plugin
    RemoteSePluginImpl plugin = (RemoteSePluginImpl) masterAPI.getPlugin();

    ProxyReader reader =
        plugin.createVirtualReader(
            CLIENT_NODE_ID,
            NATIVE_READER_NAME,
            Integration.getFakeDtoNode(),
            true,
            false,
            new HashMap<String, String>());

    reader.transmitSeRequests(
        StubReaderTest.getRequestIsoDepSetSample(),
        MultiSelectionProcessing.FIRST_MATCH,
        ChannelControl.KEEP_OPEN);
  }

  /**
   * Successful transmitSeRequests with MultiSelectionProcessing and ChannelControl
   *
   * @throws Exception
   */
  @Test
  @Ignore
  // TODO Mock does not work, see this#connectMockReader()
  // execute at hand and check logs
  public void transmitSeRequests_withParameters() throws Exception {
    List<CardRequest> cardRequests = SampleFactory.getASeRequestList();

    // test transmitSeRequests with Parameters
    ((ProxyReader) virtualReader)
        .transmitSeRequests(
            cardRequests, MultiSelectionProcessing.PROCESS_ALL, ChannelControl.CLOSE_AFTER);

    // condition -> the nativeReader execute the method executed on the virtual reader
    verify(nativeReader, times(1))
        .transmitSeRequests(
            cardRequests, MultiSelectionProcessing.PROCESS_ALL, ChannelControl.CLOSE_AFTER);
  }

  @Test
  @Ignore
  // TODO Mock does not work, see this#connectMockReader()
  // execute at hand and check logs
  public void transmitSeRequests_withNoParameters() throws Exception {
    List<CardRequest> cardRequests = SampleFactory.getASeRequestList();

    // test transmitSeRequest without parameter
    ((ProxyReader) virtualReader)
        .transmitSeRequests(
            cardRequests, MultiSelectionProcessing.FIRST_MATCH, ChannelControl.KEEP_OPEN);

    // condition -> the nativeReader execute the method executed on the virtual reader
    verify(nativeReader, times(1))
        .transmitSeRequests(
            cardRequests,
            MultiSelectionProcessing.FIRST_MATCH,
            ChannelControl.KEEP_OPEN); // default value
    // when no param is
    // specified
  }

  /**
   * Successful Transmit with ChannelControl
   *
   * @throws Exception
   */
  @Test
  @Ignore
  // TODO Mock does not work, see this#connectMockReader()
  // execute at hand and check logs
  public void transmitSeRequest_withParameters() throws Exception {
    CardRequest cardRequest = SampleFactory.getASeRequest();

    // test transmitSeRequest with Parameters
    ((ProxyReader) virtualReader).transmitSeRequest(cardRequest, ChannelControl.KEEP_OPEN);

    // condition -> the nativeReader execute the method executed on the virtual reader
    verify(nativeReader, times(1)).transmitSeRequest(cardRequest, ChannelControl.KEEP_OPEN);
  }

  @Test
  @Ignore
  // TODO Mock does not work, see this#connectMockReader()
  // execute at hand and check logs
  public void transmitSeRequest_withNoParam() throws Exception {
    CardRequest cardRequest = SampleFactory.getASeRequest();

    // test transmitSeRequest without parameter
    ((ProxyReader) virtualReader).transmitSeRequest(cardRequest, ChannelControl.KEEP_OPEN);

    // condition -> the nativeReader execute the method executed on the virtual reader
    verify(nativeReader, times(1))
        .transmitSeRequest(cardRequest, ChannelControl.KEEP_OPEN); // default
    // value when
    // no param is
    // specified
  }

  protected ProxyReader connectMockReader(String readerName) throws Exception {

    // configure mock native reader
    ProxyReader mockReader = Mockito.spy(ProxyReader.class);
    doReturn(readerName).when(mockReader).getName();
    doReturn(true).when(mockReader).isContactless();
    doReturn(new ArrayList<CardResponse>())
        .when(mockReader)
        .transmitSeRequests(
            ArgumentMatchers.<CardRequest>anyList(),
            any(MultiSelectionProcessing.class),
            any(ChannelControl.class));

    // Configure slaveAPI to find mockReader
    // TODO : findLocalReader real method is called, the mock does not work maybe due to
    // multiple thread...
    doReturn(mockReader).when(slaveAPI).findLocalReader(any(String.class));
    doCallRealMethod().when(slaveAPI).connectReader(any(Reader.class));
    doCallRealMethod().when(slaveAPI).connectReader(any(Reader.class), any(Map.class));

    slaveAPI.connectReader(mockReader);

    return mockReader;
  }
}
