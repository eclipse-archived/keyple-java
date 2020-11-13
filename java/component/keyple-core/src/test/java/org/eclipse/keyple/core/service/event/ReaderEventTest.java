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
package org.eclipse.keyple.core.service.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/** */
public class ReaderEventTest {

  @Test
  public void testGetEvent() {
    ReaderEvent event =
        new ReaderEvent("plugin", "reader", ReaderEvent.EventType.CARD_INSERTED, null);
    assertThat(event.getReaderName()).isEqualTo("reader");
    assertThat(event.getPluginName()).isEqualTo("plugin");
    assertThat(event.getEventType()).isEqualTo(ReaderEvent.EventType.CARD_INSERTED);
  }
}
