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
package org.eclipse.keyple.calypso.transaction;

import org.eclipse.keyple.calypso.command.sam.SamRevision;
import org.eclipse.keyple.core.card.selection.CardSelector;

/**
 * The {@link SamSelector} class extends {@link CardSelector} to handle specific Calypso SAM needs
 * such as model identification.
 */
public class SamSelector extends CardSelector {
  private final SamRevision targetSamRevision;
  private final byte[] unlockData;

  /** Private constructor */
  private SamSelector(SamSelectorBuilder builder) {
    super(builder);
    String atrRegex;
    String snRegex;
    /* check if serialNumber is defined */
    if (builder.serialNumber == null || builder.serialNumber.isEmpty()) {
      /* match all serial numbers */
      snRegex = ".{8}";
    } else {
      /* match the provided serial number (could be a regex substring) */
      snRegex = builder.serialNumber;
    }
    unlockData = builder.unlockData;
    targetSamRevision = builder.samRevision;
    /*
     * build the final Atr regex according to the SAM subtype and serial number if any.
     *
     * The header is starting with 3B, its total length is 4 or 6 bytes (8 or 10 hex digits)
     */
    switch (targetSamRevision) {
      case C1:
      case S1D:
      case S1E:
        atrRegex =
            "3B(.{6}|.{10})805A..80"
                + targetSamRevision.getApplicationTypeMask()
                + "20.{4}"
                + snRegex
                + "829000";
        break;
      case AUTO:
        /* match any ATR */
        atrRegex = ".*";
        break;
      default:
        throw new IllegalArgumentException("Unknown SAM subtype.");
    }
    this.getAtrFilter().setAtrRegex(atrRegex);
  }

  /**
   * Builder of {@link SamSelector}
   *
   * @since 0.9
   */
  public static final class SamSelectorBuilder extends CardSelector.CardSelectorBuilder {
    private SamRevision samRevision;
    private String serialNumber;
    private byte[] unlockData;

    public SamSelectorBuilder() {
      super();
      // set an empty AtrFilter by default
      this.atrFilter(new AtrFilter(""));
    }

    /**
     * Sets the SAM revision
     *
     * @param samRevision the {@link SamRevision} of the targeted SAM
     * @return the builder instance
     */
    public SamSelectorBuilder samRevision(SamRevision samRevision) {
      this.samRevision = samRevision;
      return this;
    }

    /**
     * Sets the SAM serial number regex
     *
     * @param serialNumber the serial number of the targeted SAM as regex
     * @return the builder instance
     */
    public SamSelectorBuilder serialNumber(String serialNumber) {
      this.serialNumber = serialNumber;
      return this;
    }

    /**
     * Sets the SAM identifier
     *
     * @param samIdentifier the {@link SamIdentifier} of the targeted SAM
     * @return the builder instance
     */
    public SamSelectorBuilder samIdentifier(SamIdentifier samIdentifier) {
      samRevision = samIdentifier.getSamRevision();
      serialNumber = samIdentifier.getSerialNumber();
      return this;
    }

    /**
     * Sets the unlock data
     *
     * @param unlockData a byte array containing the unlock data (8 or 16 bytes)
     * @return the builder instance
     * @throws IllegalArgumentException if the provided buffer size is not 8 or 16
     */
    public SamSelectorBuilder unlockData(byte[] unlockData) {
      if (unlockData == null || (unlockData.length != 8 && unlockData.length != 16)) {
        throw new IllegalArgumentException("Bad unlock data length. Should be 8 or 16 bytes.");
      }
      this.unlockData = unlockData;
      return this;
    }

    /** {@inheritDoc} */
    @Override
    public SamSelectorBuilder cardProtocol(String cardProtocol) {
      return (SamSelectorBuilder) super.cardProtocol(cardProtocol);
    }

    /** {@inheritDoc} */
    @Override
    public SamSelectorBuilder atrFilter(AtrFilter atrFilter) {
      return (SamSelectorBuilder) super.atrFilter(atrFilter);
    }

    /** {@inheritDoc} */
    @Override
    public SamSelectorBuilder aidSelector(AidSelector aidSelector) {
      return (SamSelectorBuilder) super.aidSelector(aidSelector);
    }

    /**
     * Build a new {@code SamSelector}.
     *
     * @return a new instance
     */
    @Override
    public SamSelector build() {
      return new SamSelector(this);
    }
  }

  /**
   * Gets a new builder.
   *
   * @return a new builder instance
   */
  public static SamSelectorBuilder builder() {
    return new SamSelectorBuilder();
  }

  /**
   * Gets the targeted SAM revision
   *
   * @return the target SAM revision value
   */
  public SamRevision getTargetSamRevision() {
    return targetSamRevision;
  }

  /**
   * Gets the SAM unlock data
   *
   * @return a byte array containing the unlock data or null if the unlock data is not set
   */
  public byte[] getUnlockData() {
    return unlockData;
  }
}
