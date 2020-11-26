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
package org.eclipse.keyple.plugin.remote.integration.common.app;

import org.eclipse.keyple.core.service.event.ReaderEvent;
import org.eclipse.keyple.plugin.remote.integration.common.model.UserInput;
import org.eclipse.keyple.plugin.remote.integration.common.model.UserOutputDataDto;
import org.eclipse.keyple.plugin.remote.spi.DoNotPropagateEventException;
import org.eclipse.keyple.plugin.remote.spi.ObservableReaderEventFilter;

public class ReaderEventFilter implements ObservableReaderEventFilter {

  public UserOutputDataDto transactionResult;
  public UserInput user;

  public void setUserData(UserInput user) {
    this.user = user;
  }

  public boolean resetUserOutputDataDto() {
    transactionResult = null;
    return true;
  }

  @Override
  public Object beforePropagation(ReaderEvent event) throws DoNotPropagateEventException {
    switch (event.getEventType()) {
      case CARD_MATCHED:
        return new UserInput().setUserId(user.getUserId());
      case CARD_REMOVED:
        // return null;//send null to server
      case CARD_INSERTED:
      default:
        throw new DoNotPropagateEventException("only SE_MATCHED are propagated");
    }
  }

  @Override
  public Class<? extends Object> getUserOutputDataClass() {
    return UserOutputDataDto.class;
  }

  @Override
  public void afterPropagation(Object userOutputData) {
    if (userOutputData != null) {
      transactionResult = (UserOutputDataDto) userOutputData;
    }
  }
};
