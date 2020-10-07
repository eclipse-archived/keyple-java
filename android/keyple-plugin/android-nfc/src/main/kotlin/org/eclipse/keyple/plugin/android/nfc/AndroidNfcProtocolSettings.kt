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
package org.eclipse.keyple.plugin.android.nfc

import org.eclipse.keyple.core.util.ContactlessCardCommonProtocols

/**
 * This class contains all the parameters to identify the communication protocols supported by NFC
 * readers.
 *
 *
 * The application can choose to add all parameters or only a subset.
 */
object AndroidNfcProtocolSettings {

    /**
     * Associates a protocol and a string defining how to identify it (here a regex to be applied on
     * the ATR)
     */
    private val allSettings: Map<String, String> = mapOf(
        ContactlessCardCommonProtocols.ISO_14443_4.name to "android.nfc.tech.IsoDep",
        ContactlessCardCommonProtocols.NFC_A_ISO_14443_3A.name to "android.nfc.tech.NfcA",
        ContactlessCardCommonProtocols.NFC_B_ISO_14443_3B.name to "android.nfc.tech.NfcB",
        ContactlessCardCommonProtocols.NFC_F_JIS_6319_4.name to "android.nfc.tech.NfcF",
        ContactlessCardCommonProtocols.NFC_V_ISO_15693.name to "android.nfc.tech.NfcV",
        ContactlessCardCommonProtocols.NFC_NDEF_TAG.name to "android.nfc.tech.Ndef",
        ContactlessCardCommonProtocols.NFC_NDEF_FORMATABLE.name to "android.nfc.tech.NdefFormatable",
        ContactlessCardCommonProtocols.NFC_BARCODE.name to "android.nfc.tech.NfcBarcode",
        ContactlessCardCommonProtocols.MIFARE_ULTRA_LIGHT.name to "android.nfc.tech.MifareUltralight",
        ContactlessCardCommonProtocols.MIFARE_CLASSIC.name to "android.nfc.tech.MifareClassic")

    /**
     * Return a subset of the settings map
     *
     * @param specificProtocols
     * @return a settings map
     */
    @Throws(NoSuchElementException::class)
    fun getSpecificSettings(specificProtocols: Set<String>):
            Map<String, String> {
        return specificProtocols.filter { allSettings[it] != null }.associateBy({ it }, { getSetting(it) })
    }

    /**
     * Associates a protocol and a string defining how to identify it (here a regex to be applied on
     * the ATR)
     *
     * @return a settings map
     * @throws No such Element Exception if protocol not found in settings
     */
    @Throws(NoSuchElementException::class)
    fun getSetting(seProtocol: String): String {
        return allSettings.getValue(seProtocol)
    }
}
