/********************************************************************************
 * Copyright (c) 2019 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.keyple.core.seproxy.event;

import org.eclipse.keyple.core.seproxy.message.SeResponseSet;

public abstract class AbstractDefaultSelectionsResponse {
    /** The {@link org.eclipse.keyple.core.seproxy.message.SeResponseSet} */
    protected final SeResponseSet selectionSeResponseSet;

    protected AbstractDefaultSelectionsResponse(SeResponseSet selectionSeResponseSet) {
        this.selectionSeResponseSet = selectionSeResponseSet;
    }

    public abstract SeResponseSet getSelectionSeResponseSet();
}
