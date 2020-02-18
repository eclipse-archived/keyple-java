/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.keyple.plugin.android.omapi.simalliance

import android.content.Context
import org.eclipse.keyple.core.seproxy.SeReader
import org.eclipse.keyple.plugin.android.omapi.AbstractAndroidOmapiPlugin
import org.eclipse.keyple.plugin.android.omapi.PLUGIN_NAME
import org.simalliance.openmobileapi.Reader
import org.simalliance.openmobileapi.SEService
import timber.log.Timber

/**
 * Allow to provide an implementation of AbstractAndroidOmapiPlugin using the Simalliance
 * OMAPI implementation of Reader and SeService objects.
 */
internal object AndroidOmapiPlugin : AbstractAndroidOmapiPlugin<Reader, SEService>(), SEService.CallBack {

    override fun connectToSe(context: Context) {
        val seServiceFactory = SeServiceFactoryImpl(context)
        seService = seServiceFactory.connectToSe(this)
        Timber.i("OMAPI SEService version: %s", seService?.version)
    }

    override fun getNativeReaders(): Array<Reader>? {
        return seService?.readers
    }

    override fun mapToSeReader(nativeReader: Reader): SeReader {
        Timber.d("Reader available name : %s", nativeReader.name)
        Timber.d("Reader available isSePresent : %S", nativeReader.isSecureElementPresent)
        return AndroidOmapiReader(nativeReader, PLUGIN_NAME, nativeReader.name)
    }

    /**
     * Warning. Do not call this method directly.
     *
     * Invoked by Open Mobile {@link SEService} when connected
     * Instantiates {@link AndroidOmapiReader} for each SE Reader detected in the platform
     *
     * @param seService : connected omapi service
     */
    override fun serviceConnected(p0: SEService?) {
        Timber.i("Retrieve available readers...")

        // init readers
        readers = initNativeReaders()
    }
}
