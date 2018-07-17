/*
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License version 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 */

package org.eclipse.keyple.calypso.commands.po.builder;

import java.nio.ByteBuffer;
import org.eclipse.keyple.calypso.commands.PoSendableInSession;
import org.eclipse.keyple.calypso.commands.po.AbstractPoCommandBuilder;
import org.eclipse.keyple.calypso.commands.po.CalypsoPoCommands;
import org.eclipse.keyple.calypso.commands.po.PoRevision;
import org.eclipse.keyple.calypso.commands.utils.RequestUtils;
import org.eclipse.keyple.commands.CommandsTable;
import org.eclipse.keyple.seproxy.ApduRequest;

// TODO: Auto-generated Javadoc
/**
 * The Class AppendRecordCmdBuild. This class provides the dedicated constructor to build the Update
 * Record APDU command.
 *
 * @author Ixxi
 *
 */
public class AppendRecordCmdBuild extends AbstractPoCommandBuilder implements PoSendableInSession {

    /** The command. */
    private static CommandsTable command = CalypsoPoCommands.APPEND_RECORD;

    /**
     * Instantiates a new append record cmd build.
     *
     * @param commandeReference the commande reference
     * @param request the request
     * @throws java.lang.IllegalArgumentException - if the command is inconsistent
     */
    AppendRecordCmdBuild(CommandsTable commandeReference, ApduRequest request)
            throws IllegalArgumentException {
        super(commandeReference, request);
    }

    /**
     * Instantiates a new UpdateRecordCmdBuild.
     *
     * @param revision the revision of the PO
     * @param sfi the sfi to select
     * @param newRecordData the new record data to write
     * @throws java.lang.IllegalArgumentException - if the command is inconsistent
     */
    public AppendRecordCmdBuild(PoRevision revision, byte sfi, ByteBuffer newRecordData) {
        super(command, null);
        if (revision != null) {
            this.defaultRevision = revision;
        }
        byte cla = PoRevision.REV2_4.equals(this.defaultRevision) ? (byte) 0x94 : (byte) 0x00;
        byte p1 = (byte) 0x00;
        byte p2 = (sfi == 0) ? (byte) 0x00 : (byte) (sfi * 8);

        this.request = RequestUtils.constructAPDURequest(cla, command, p1, p2, newRecordData);
    }


    /**
     * Instantiates a new append record cmd build.
     *
     * @param request the request
     * @throws java.lang.IllegalArgumentException - if the request is inconsistent
     */
    public AppendRecordCmdBuild(ApduRequest request) throws IllegalArgumentException {
        super(CalypsoPoCommands.APPEND_RECORD, request);
        RequestUtils.controlRequestConsistency(command, request);
    }

}
