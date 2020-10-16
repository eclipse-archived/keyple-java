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
package org.eclipse.keyple.core.selection;

import java.util.*;
import org.eclipse.keyple.core.command.AbstractApduCommandBuilder;
import org.eclipse.keyple.core.reader.MultiSelectionProcessing;
import org.eclipse.keyple.core.reader.Reader;
import org.eclipse.keyple.core.reader.event.AbstractDefaultSelectionsRequest;
import org.eclipse.keyple.core.reader.event.AbstractDefaultSelectionsResponse;
import org.eclipse.keyple.core.reader.event.ReaderEvent;
import org.eclipse.keyple.core.reader.exception.KeypleException;
import org.eclipse.keyple.core.reader.exception.KeypleReaderIOException;
import org.eclipse.keyple.core.reader.message.CardRequest;
import org.eclipse.keyple.core.reader.message.CardResponse;
import org.eclipse.keyple.core.reader.message.ChannelControl;
import org.eclipse.keyple.core.reader.message.DefaultSelectionsRequest;
import org.eclipse.keyple.core.reader.message.DefaultSelectionsResponse;
import org.eclipse.keyple.core.reader.message.ProxyReader;
import org.eclipse.keyple.core.reader.message.SelectionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The CardSelection class handles the card selection process.
 *
 * <p>It provides a way to do an explicit card selection or to post process a default card
 * selection. <br>
 * The channel is kept open by default, but can be closed after each selection cases (see
 * PrepareReleaseSeChannel).
 */
public final class CardSelection {
  private static final Logger logger = LoggerFactory.getLogger(CardSelection.class);

  /*
   * list of selection requests used to build the AbstractSmartCard list in return of
   * processSelection methods
   */
  private final List<AbstractCardSelectionRequest<? extends AbstractApduCommandBuilder>>
      cardSelectionRequests =
          new ArrayList<AbstractCardSelectionRequest<? extends AbstractApduCommandBuilder>>();
  private final MultiSelectionProcessing multiSelectionProcessing;
  private ChannelControl channelControl = ChannelControl.KEEP_OPEN;

  /**
   * Constructor.
   *
   * @param multiSelectionProcessing the multi card processing mode
   */
  public CardSelection(MultiSelectionProcessing multiSelectionProcessing) {
    this.multiSelectionProcessing = multiSelectionProcessing;
  }

  /** Alternate constructor for standard usages. */
  public CardSelection() {
    this(MultiSelectionProcessing.FIRST_MATCH);
  }

  /**
   * Prepare a selection: add the selection request from the provided selector to the selection
   * request set.
   *
   * <p>
   *
   * @param cardSelectionRequest the selector to prepare
   * @return the selection index giving the current selection position in the selection request.
   */
  public int prepareSelection(
      AbstractCardSelectionRequest<? extends AbstractApduCommandBuilder> cardSelectionRequest) {
    if (logger.isTraceEnabled()) {
      logger.trace("SELECTIONREQUEST = {}", cardSelectionRequest.getSelectionRequest());
    }
    /* keep the selection request */
    cardSelectionRequests.add(cardSelectionRequest);
    /* return the selection index (starting at 0) */
    return cardSelectionRequests.size() - 1;
  }

  /**
   * Prepare to close the card channel.<br>
   * If this command is called before a "process" selection command then the last transmission to
   * the PO will be associated with the indication CLOSE_AFTER in order to close the card channel.
   * <br>
   * This makes it possible to chain several selections on the same card if necessary.
   */
  public final void prepareReleaseChannel() {
    channelControl = ChannelControl.CLOSE_AFTER;
  }

  /**
   * Process the selection response either from a {@link ReaderEvent} (default selection) or from an
   * explicit selection.
   *
   * <p>The responses from the List of {@link CardResponse} is parsed and checked.
   *
   * <p>A {@link AbstractSmartCard} list is build and returned. Non matching card are signaled by a
   * null element in the list
   *
   * @param defaultSelectionsResponse the selection response
   * @return the {@link SelectionsResult} containing the result of all prepared selection cases,
   *     including {@link AbstractSmartCard} and {@link CardResponse}.
   * @throws KeypleException if the selection process failed
   */
  private SelectionsResult processSelection(
      AbstractDefaultSelectionsResponse defaultSelectionsResponse) {
    SelectionsResult selectionsResult = new SelectionsResult();

    int index = 0;

    /* Check card responses */
    for (CardResponse cardResponse :
        ((DefaultSelectionsResponse) defaultSelectionsResponse).getSelectionCardResponses()) {
      /* test if the selection is successful: we should have either a FCI or an ATR */
      if (cardResponse != null
          && cardResponse.getSelectionStatus() != null
          && cardResponse.getSelectionStatus().hasMatched()) {
        /*
         * create a AbstractSmartCard with the class deduced from the selection request
         * during the selection preparation
         */
        AbstractSmartCard smartCard = cardSelectionRequests.get(index).parse(cardResponse);

        // determine if the current matching card is selected
        SelectionStatus selectionStatus = cardResponse.getSelectionStatus();
        boolean isSelected;
        if (selectionStatus != null) {
          isSelected = selectionStatus.hasMatched() && cardResponse.isLogicalChannelOpen();
        } else {
          isSelected = false;
        }

        selectionsResult.addSmartCard(index, smartCard, isSelected);
      }
      index++;
    }
    return selectionsResult;
  }

  /**
   * Parses the response to a selection operation sent to a card and return a list of {@link
   * AbstractSmartCard}
   *
   * <p>Selection cases that have not matched the current card are set to null.
   *
   * @param defaultSelectionsResponse the response from the reader to the {@link
   *     AbstractDefaultSelectionsRequest}
   * @return the {@link SelectionsResult} containing the result of all prepared selection cases,
   *     including {@link AbstractSmartCard} and {@link CardResponse}.
   * @throws KeypleException if an error occurs during the selection process
   */
  public SelectionsResult processDefaultSelection(
      AbstractDefaultSelectionsResponse defaultSelectionsResponse) {

    /* null pointer exception protection */
    if (defaultSelectionsResponse == null) {
      logger.error("defaultSelectionsResponse shouldn't be null in processSelection.");
      return null;
    }

    if (logger.isTraceEnabled()) {
      logger.trace(
          "Process default SELECTIONRESPONSE ({} response(s))",
          ((DefaultSelectionsResponse) defaultSelectionsResponse)
              .getSelectionCardResponses()
              .size());
    }

    return processSelection(defaultSelectionsResponse);
  }

  /**
   * Execute the selection process and return a list of {@link AbstractSmartCard}.
   *
   * <p>Selection requests are transmitted to the card through the supplied Reader.
   *
   * <p>The process stops in the following cases:
   *
   * <ul>
   *   <li>All the selection requests have been transmitted
   *   <li>A selection request matches the current card and the keepChannelOpen flag was true
   * </ul>
   *
   * <p>
   *
   * @param reader the Reader on which the selection is made
   * @return the {@link SelectionsResult} containing the result of all prepared selection cases,
   *     including {@link AbstractSmartCard} and {@link CardResponse}.
   * @throws KeypleReaderIOException if the communication with the reader or the card has failed
   * @throws KeypleException if an error occurs during the selection process
   */
  public SelectionsResult processExplicitSelection(Reader reader) {
    List<CardRequest> selectionRequests = new ArrayList<CardRequest>();
    for (AbstractCardSelectionRequest<? extends AbstractApduCommandBuilder> cardSelectionRequest :
        cardSelectionRequests) {
      selectionRequests.add(cardSelectionRequest.getSelectionRequest());
    }
    if (logger.isTraceEnabled()) {
      logger.trace("Transmit SELECTIONREQUEST ({} request(s))", selectionRequests.size());
    }

    /* Communicate with the card to do the selection */
    List<CardResponse> cardResponse =
        ((ProxyReader) reader)
            .transmitCardRequests(selectionRequests, multiSelectionProcessing, channelControl);

    return processSelection(new DefaultSelectionsResponse(cardResponse));
  }

  /**
   * The SelectionOperation is the {@link AbstractDefaultSelectionsRequest} to process in ordered to
   * select a card among others through the selection process. This method is useful to build the
   * prepared selection to be executed by a reader just after a card insertion.
   *
   * @return the {@link AbstractDefaultSelectionsRequest} previously prepared with prepareSelection
   */
  public AbstractDefaultSelectionsRequest getSelectionOperation() {
    List<CardRequest> selectionRequests = new ArrayList<CardRequest>();
    for (AbstractCardSelectionRequest<? extends AbstractApduCommandBuilder> cardSelectionRequest :
        cardSelectionRequests) {
      selectionRequests.add(cardSelectionRequest.getSelectionRequest());
    }
    return new DefaultSelectionsRequest(
        selectionRequests, multiSelectionProcessing, channelControl);
  }
}
