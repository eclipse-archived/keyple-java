/********************************************************************************
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.keyple.core.selection;

import org.eclipse.keyple.core.seproxy.event.AbstractDefaultSelectionsResponse;
import org.eclipse.keyple.core.seproxy.event.ObservableReader;
import org.eclipse.keyple.core.seproxy.message.SeResponseSet;

/**
 * Class containing the {@link SeResponseSet} used from a default selection made at the
 * {@link ObservableReader} level.
 */
public class DefaultSelectionsResponse extends AbstractDefaultSelectionsResponse {
    /** The {@link SeResponseSet} */
    private final SeResponseSet selectionSeResponseSet;

    public DefaultSelectionsResponse(SeResponseSet selectionSeResponseSet) {
        this.selectionSeResponseSet = selectionSeResponseSet;
    }

    @Override
    public SeResponseSet getSelectionSeResponseSet() {
        return selectionSeResponseSet;
    }
}
