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

import org.eclipse.keyple.core.card.selection.CardSelector;

/**
 * This POJO contains the information needed to select a particular PO and specify the behavior in
 * case of invalidation.
 *
 * @since 0.9
 */
public final class PoSelector extends CardSelector {
  private static final int SW_PO_INVALIDATED = 0x6283;

  /**
   * Indicates if an invalidated PO should be selected or not.
   *
   * <p>The acceptance of an invalid PO is determined with the additional successful status codes
   * specified in the {@link AidSelector}
   *
   * @since 0.9
   */
  public enum InvalidatedPo {
    REJECT,
    ACCEPT
  }

  /** Private constructor */
  private PoSelector(PoSelectorBuilder builder) {
    super(builder);
    if (builder.invalidatedPo == InvalidatedPo.ACCEPT) {
      this.getAidSelector().addSuccessfulStatusCode(SW_PO_INVALIDATED);
    }
  }

  /**
   * Builder class for {@link PoSelector}
   *
   * @since 0.9
   */
  public static final class PoSelectorBuilder extends CardSelector.CardSelectorBuilder {
    private InvalidatedPo invalidatedPo;

    private PoSelectorBuilder() {
      super();
    }

    /**
     * Sets the desired behaviour in case of invalidated POs
     *
     * @param invalidatedPo the {@link InvalidatedPo} wanted behaviour
     * @return the builder instance
     * @since 0.9
     */
    public PoSelectorBuilder invalidatedPo(InvalidatedPo invalidatedPo) {
      this.invalidatedPo = invalidatedPo;
      return this;
    }

    /**
     * {@inheritDoc}
     *
     * @since 0.9
     */
    @Override
    public PoSelectorBuilder cardProtocol(String cardProtocol) {
      return (PoSelectorBuilder) super.cardProtocol(cardProtocol);
    }

    /**
     * {@inheritDoc}
     *
     * @since 0.9
     */
    @Override
    public PoSelectorBuilder atrFilter(AtrFilter atrFilter) {
      return (PoSelectorBuilder) super.atrFilter(atrFilter);
    }

    /**
     * {@inheritDoc}
     *
     * @since 0.9
     */
    @Override
    public PoSelectorBuilder aidSelector(AidSelector aidSelector) {
      return (PoSelectorBuilder) super.aidSelector(aidSelector);
    }

    /**
     * Builds a new {@code PoSelector}.
     *
     * @return a new instance
     * @since 0.9
     */
    @Override
    public PoSelector build() {
      return new PoSelector(this);
    }
  }

  /**
   * Gets a new builder.
   *
   * @return a new builder instance
   * @since 0.9
   */
  public static PoSelectorBuilder builder() {
    return new PoSelectorBuilder();
  }
}
