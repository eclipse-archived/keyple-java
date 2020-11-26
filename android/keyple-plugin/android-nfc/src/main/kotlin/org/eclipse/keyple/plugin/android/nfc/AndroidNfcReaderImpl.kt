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

import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import java.io.IOException
import java.util.HashMap
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderIOException
import org.eclipse.keyple.core.seproxy.plugin.reader.AbstractObservableLocalReader
import org.eclipse.keyple.core.seproxy.plugin.reader.ObservableReaderStateService
import org.eclipse.keyple.core.util.ByteArrayUtil
import timber.log.Timber

/**
 * Implementation of [AndroidNfcReader] based on keyple core abstract classes [AbstractObservableLocalReader]
 * and
 */
internal object AndroidNfcReaderImpl : AbstractObservableLocalReader(AndroidNfcReader.PLUGIN_NAME, AndroidNfcReader.READER_NAME), AndroidNfcReader, NfcAdapter.ReaderCallback {

    // Android NFC Adapter
    private var nfcAdapter: NfcAdapter? = null

    // keep state between session if required
    private var tagProxy: TagProxy? = null

    private val parameters = HashMap<String, String?>()

    private val protocolsMap = HashMap<String, String?>()

    private const val NO_TAG = "no-tag"

    private var isWatingForRemoval = false
    private val syncWaitRemoval = Object()

    /**
     * Build Reader Mode flags Integer from parameters
     *
     * @return flags Integer
     */
    // Build flags list for reader mode
    val flags: Int
        get() {

            var flags = 0

            val ndef = parameters[AndroidNfcReader.FLAG_READER_SKIP_NDEF_CHECK]
            val nosounds = parameters[AndroidNfcReader.FLAG_READER_NO_PLATFORM_SOUNDS]

            if (ndef != null && ndef == "1") {
                flags = flags or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
            }

            if (nosounds != null && nosounds == "1") {
                flags = flags or NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
            }
            for (cardProtocol in this.protocolsMap.keys) {
                if (AndroidNfcSupportedProtocols.ISO_14443_4.name == cardProtocol) {
                    flags = flags or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_NFC_A
                } else if (AndroidNfcSupportedProtocols.MIFARE_ULTRA_LIGHT.name == cardProtocol || AndroidNfcSupportedProtocols.MIFARE_CLASSIC.name == cardProtocol) {
                    flags = flags or NfcAdapter.FLAG_READER_NFC_A
                }
            }

            return flags
        }

    /**
     * Build Reader Mode options Bundle from parameters
     *
     * @return options
     */
    val options: Bundle
        get() {
            val options = Bundle(1)
            if (parameters.containsKey(AndroidNfcReader.FLAG_READER_PRESENCE_CHECK_DELAY)) {
                val delay = Integer
                        .parseInt(parameters[AndroidNfcReader.FLAG_READER_PRESENCE_CHECK_DELAY]!!)
                options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, delay)
            }
            return options
        }

    override fun initStateService(): ObservableReaderStateService {

        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            ObservableReaderStateService.builder(this)
                    .WaitForCardInsertionWithNativeDetection()
                    .WaitForCardProcessingWithNativeDetection()
                    .WaitForCardRemovalWithPollingDetection()
                    .build()
        } else {
            ObservableReaderStateService.builder(this)
                    .WaitForCardInsertionWithNativeDetection()
                    .WaitForCardProcessingWithNativeDetection()
                    .WaitForCardRemovalWithSmartDetection()
                    .build()
        }
    }

    /**
     * Get Reader parameters
     *
     * @return parameters
     */
    override fun getParameters(): Map<String, String?> {
        return parameters
    }

    /**
     * Configure NFC Reader AndroidNfcReaderImpl supports the following parameters : FLAG_READER:
     * SKIP_NDEF_CHECK (skip NDEF check when a smartcard is detected) FLAG_READER:
     * NO_PLATFORM_SOUNDS (disable device sound when nfc smartcard is detected)
     * EXTRA_READER_PRESENCE_CHECK_DELAY: "Int" (see @NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY)
     *
     * @param key the parameter key
     * @param value the parameter value
     * @throws IOException
     */
    @Throws(IllegalArgumentException::class)
    override fun setParameter(key: String, value: String) {
        Timber.i("AndroidNfcReaderImpl supports the following parameters")
        Timber.i("%s, FLAG_READER_SKIP_NDEF_CHECK:%s, FLAG_READER_NO_PLATFORM_SOUNDS:%s, FLAG_READER_PRESENCE_CHECK_DELAY:%s",
                AndroidNfcReader.READER_NAME,
                parameters[AndroidNfcReader.FLAG_READER_SKIP_NDEF_CHECK],
                parameters[AndroidNfcReader.FLAG_READER_NO_PLATFORM_SOUNDS],
                parameters[AndroidNfcReader.FLAG_READER_PRESENCE_CHECK_DELAY])

        val correctParameter = (key == AndroidNfcReader.FLAG_READER_SKIP_NDEF_CHECK && value == "1" || value == "0" ||
                key == AndroidNfcReader.FLAG_READER_NO_PLATFORM_SOUNDS && value == "1" || value == "0" ||
                key == AndroidNfcReader.FLAG_READER_PRESENCE_CHECK_DELAY && Integer.parseInt(value) > -1)

        if (correctParameter) {
            Timber.w("Adding parameter : $key - $value")
            parameters[key] = value
        } else {
            Timber.w("Unrecognized parameter : $key")
            throw IllegalArgumentException("Unrecognized parameter $key : $value")
        }
    }

    /**
     * The transmission mode is always CONTACTLESS in a NFC reader
     *
     * @return Always true.
     */
    override fun isContactless(): Boolean {
        return true
    }

    /**
     * Callback function invoked by @[NfcAdapter] when a @[Tag] is discovered A
     * TagTransceiver is created based on the Tag technology see [TagProxy.getTagProxy]
     * Do not call this function directly.
     *
     * @param tag : detected tag
     */
    override fun onTagDiscovered(tag: Tag?) {
        Timber.i("Received Tag Discovered event $tag")
        tag?.let {
            try {
                Timber.i("Getting tag proxy")
                tagProxy = TagProxy.getTagProxy(tag)
                onEvent(InternalEvent.CARD_INSERTED)
            } catch (e: KeypleReaderException) {
                Timber.e(e)
            }
        }
    }

    /**
     *
     * @return true if a card is present
     */
    public override fun checkSePresence(): Boolean {
        return tagProxy != null
    }

    public override fun getATR(): ByteArray? {
        val atr = tagProxy?.atr
        Timber.d("ATR : ${ByteArrayUtil.toHex(atr)}")
        return if (atr?.isNotEmpty() == true) atr else null
    }

    public override fun isPhysicalChannelOpen(): Boolean {
        return tagProxy?.isConnected == true
    }

    @Throws(KeypleReaderIOException::class)
    public override fun openPhysicalChannel() {
        if (tagProxy?.isConnected != true) {
            try {
                Timber.d("Connect to tag..")
                tagProxy?.connect()
                Timber.i("Tag connected successfully : ${printTagId()}")
            } catch (e: IOException) {
                Timber.e(e, "Error while connecting to Tag ")
                throw KeypleReaderIOException("Error while opening physical channel", e)
            }
        } else {
            Timber.i("Tag is already connected to : ${printTagId()}")
        }
    }

    @Throws(KeypleReaderIOException::class)
    public override fun closePhysicalChannel() {
        try {
            tagProxy?.close()
            Timber.i("Disconnected tag : ${printTagId()}")
        } catch (e: IOException) {
            Timber.e(e, "Disconnecting error")
            throw KeypleReaderIOException("Error while closing physical channel", e)
        } finally {
            tagProxy = null
        }
    }

    @Throws(KeypleReaderIOException::class)
    public override fun transmitApdu(apduIn: ByteArray): ByteArray {
        Timber.d("Send data to card : ${apduIn.size} bytes")
        return with(tagProxy) {
            if (this == null) {
                throw KeypleReaderIOException(
                        "Error while transmitting APDU, invalid out data buffer")
            } else {
                try {
                    val bytes = transceive(apduIn)
                    if (bytes.size <2) {
                        throw KeypleReaderIOException(
                                "Error while transmitting APDU, invalid out data buffer")
                    } else {
                        Timber.d("Receive data from card : ${ByteArrayUtil.toHex(bytes)}")
                        bytes
                    }
                } catch (e: IOException) {
                    throw KeypleReaderIOException(
                            "Error while transmitting APDU, invalid out data buffer", e)
                } catch (e: NoSuchElementException) {
                    throw KeypleReaderIOException("Error while transmitting APDU, no such Element", e)
                }
            }
        }
    }

    /**
     * Activates the provided card protocol.
     *
     *
     *  * Ask the plugin to take this protocol into account if a card using this protocol is
     * identified during the selection phase.
     *  * Activates the detection of SEs using this protocol (if the plugin allows it).
     *
     *
     * @param readerProtocolName The protocol to activate (must be not null).
     * @throws KeypleReaderProtocolNotSupportedException if the protocol is not supported.
     */
    override fun activateReaderProtocol(readerProtocolName: String?) {
        TODO("Not yet implemented")
    }

    /**
     * Deactivates the provided card protocol.
     *
     *
     *  * Ask the plugin to ignore this protocol if a card using this protocol is identified during
     * the selection phase.
     *  * Inhibits the detection of SEs using this protocol (if the plugin allows it).
     *
     *
     * @param readerProtocolName The protocol to deactivate (must be not null).
     */
    override fun deactivateReaderProtocol(readerProtocolName: String?) {
        TODO("Not yet implemented")
    }

    /**
     * Tells if the provided protocol matches the current protocol.
     *
     * @param readerProtocolName A not empty String.
     * @return True or false
     * @throws KeypleReaderProtocolNotFoundException if it is not possible to determine the protocol.
     * @since 1.0
     */
    override fun isCurrentProtocol(readerProtocolName: String?): Boolean {
        TODO("Not yet implemented")
        return true
    }

    /**
     * Process data from NFC Intent
     *
     * @param intent : Intent received and filterByProtocol by xml tech_list
     */
    override fun processIntent(intent: Intent) {

        // Extract Tag from Intent
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        this.onTagDiscovered(tag)
    }

    override fun printTagId(): String {
        return with(tagProxy) {
            if (this == null) {
                NO_TAG
            } else {
                // build a user friendly TechList
                val techList = tag.techList.joinToString(separator = ", ") { it.replace("android.nfc.tech.", "") }
                // build a hexa TechId
                val tagId = tag.id.joinToString(separator = " ") { String.format("%02X", it) }
                "$tagId - $techList"
            }
        }
    }

    override fun enableNFCReaderMode(activity: Activity) {
        if (nfcAdapter == null) {
            nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        }

        val flags = flags

        val options = options

        Timber.i("Enabling Read Write Mode with flags : $flags and options : $options")

        // Reader mode for NFC reader allows to listen to NFC events without the Intent mechanism.
        // It is active only when the activity thus the fragment is active.
        nfcAdapter?.enableReaderMode(activity, this, flags, options)
    }

    override fun disableNFCReaderMode(activity: Activity) {
        nfcAdapter?.disableReaderMode(activity)
    }

    override fun stopWaitForCardRemoval() {
        Timber.d("stopWaitForCardRemoval")
        isWatingForRemoval = false
        synchronized(syncWaitRemoval) {
            syncWaitRemoval.notify()
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    override fun waitForCardAbsentNative(): Boolean {
        Timber.d("waitForCardAbsentNative")
        var isRemoved = false
        if (!isWatingForRemoval) {
            isWatingForRemoval = true
            nfcAdapter?.ignore(tagProxy?.tag, 1000, {
                isRemoved = true
                synchronized(syncWaitRemoval) {
                    syncWaitRemoval.notify()
                }
            }, null)

            synchronized(syncWaitRemoval) {
                syncWaitRemoval.wait(10000)
            }
        }
        return isRemoved
    }
}
