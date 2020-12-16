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
package org.eclipse.keyple.distributed.integration.pool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.SortedSet;
import org.eclipse.keyple.calypso.transaction.CalypsoPo;
import org.eclipse.keyple.core.card.selection.CardSelectionsService;
import org.eclipse.keyple.core.service.Reader;
import org.eclipse.keyple.core.service.SmartCardService;
import org.eclipse.keyple.distributed.PoolRemotePluginClient;
import org.eclipse.keyple.distributed.impl.PoolLocalServiceServerFactory;
import org.eclipse.keyple.distributed.impl.PoolRemotePluginClientFactory;
import org.eclipse.keyple.distributed.integration.common.endpoint.pool.StubAsyncEndpointClient;
import org.eclipse.keyple.distributed.integration.common.endpoint.pool.StubAsyncEndpointServer;
import org.eclipse.keyple.distributed.integration.common.util.CalypsoUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncScenario extends BaseScenario {

  private static final Logger logger = LoggerFactory.getLogger(AsyncScenario.class);

  @Rule public TestName testName = new TestName();

  @Before
  public void setUp() {
    initNativePoolStubPlugin();

    localServiceName = testName.getMethodName() + "_async";

    StubAsyncEndpointServer serverEndpoint = new StubAsyncEndpointServer(localServiceName);
    StubAsyncEndpointClient clientEndpoint = new StubAsyncEndpointClient(serverEndpoint);

    poolLocalServiceServer =
        PoolLocalServiceServerFactory.builder()
            .withServiceName(localServiceName)
            .withAsyncNode(serverEndpoint)
            .withPoolPlugins(localPoolPlugin.getName())
            .getService();

    poolRemotePluginClient =
        (PoolRemotePluginClient)
            SmartCardService.getInstance()
                .registerPlugin(
                    PoolRemotePluginClientFactory.builder()
                        .withDefaultPluginName()
                        .withAsyncNode(clientEndpoint)
                        .usingDefaultTimeout()
                        .build());
  }

  @After
  public void tearDown() {
    SmartCardService.getInstance().unregisterPlugin(poolRemotePluginClient.getName());
  }

  @Test
  @Override
  public void execute_transaction_on_pool_reader() {
    SortedSet<String> groupReferences = poolRemotePluginClient.getReaderGroupReferences();
    assertThat(groupReferences).containsExactly(groupReference);

    Reader remoteReader = poolRemotePluginClient.allocateReader(groupReference);
    CardSelectionsService seSelection = CalypsoUtils.getCardSelection();
    CalypsoPo calypsoPo =
        (CalypsoPo) seSelection.processExplicitSelections(remoteReader).getActiveSmartCard();

    String eventLog = CalypsoUtils.readEventLog(calypsoPo, remoteReader, logger);
    assertThat(eventLog).isNotNull();
    poolRemotePluginClient.releaseReader(remoteReader);
  }
}
