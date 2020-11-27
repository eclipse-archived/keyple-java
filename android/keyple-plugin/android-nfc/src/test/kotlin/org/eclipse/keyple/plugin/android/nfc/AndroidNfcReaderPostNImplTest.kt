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

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.io.IOException
import org.eclipse.keyple.core.service.event.ReaderObservationExceptionHandler
import org.eclipse.keyple.core.service.exception.KeypleReaderException
import org.eclipse.keyple.core.service.exception.KeypleReaderIOException
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(RobolectricTestRunner::class)
class AndroidNfcReaderPostNImplTest {

    private lateinit var reader: AndroidNfcReaderPostNImpl

    private lateinit var activity: Activity

    private val readerObservationExceptionHandler = ReaderObservationExceptionHandler { pluginName, readerName, e -> }

    @MockK
    internal lateinit var tag: Tag

    @MockK
    internal var tagProxy: TagProxy? = null

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        val app: Application = ApplicationProvider.getApplicationContext()
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        reader = AndroidNfcReaderPostNImpl(activity, readerObservationExceptionHandler)

        // We need to mock tag.* because it's called in printTagId() called when channel is closed
        every { tagProxy?.tag } returns tag
        every { tag.techList } returns arrayOf("android.nfc.tech.IsoDep")
        every { tag.id } returns "00".toByteArray()
        mockkStatic(TagProxy::class)
        mockkObject(TagProxy.Companion)
        every { TagProxy.getTagProxy(tag) } returns tagProxy!!

        mockkStatic(NfcAdapter::class)
        val nfcAdapter = NfcAdapter.getDefaultAdapter(app)
        every { NfcAdapter.getDefaultAdapter(any()) } returns nfcAdapter
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun aInitReaderTest() { // Must be ran in 1st position as AndroidNfcReaderImpl is a singleton
        // Assert.assertEquals(AbstractObservableState.MonitoringState.WAIT_FOR_START_DETECTION, reader.currentMonitoringState)
        Assert.assertEquals(true, reader.isContactless)
        Assert.assertEquals(AndroidNfcPlugin.PLUGIN_NAME, reader.pluginName)
        Assert.assertEquals(AndroidNfcReader.READER_NAME, reader.name)
        Assert.assertNull((reader as AndroidNfcReader).presenceCheckDelay)
        Assert.assertNull((reader as AndroidNfcReader).noPlateformSound)
        Assert.assertNull((reader as AndroidNfcReader).skipNdefCheck)
    }

    // ---- TAG EVENTS  TESTS ----------- //

    @Test
    fun checkCardPresenceTest() {
        every { tagProxy?.isConnected } returns true
        presentMockTag()
        Assert.assertTrue(reader.checkCardPresence())
    }

    @Test
    fun processIntent() {
        reader.processIntent(Intent())
        Assert.assertTrue(true) // no test?
    }

    @Test
    fun getATR() {
        presentMockTag()
        val atr = byteArrayOf(0x90.toByte(), 0x00)
        every { tagProxy?.atr } returns atr
        Assert.assertEquals(atr, reader.atr)
    }

    // ---- PHYSICAL CHANNEL TESTS ----------- //

    @Test
    fun isPhysicalChannelOpen() {
        presentMockTag()
        every { tagProxy?.isConnected } returns true
        Assert.assertEquals(true, reader.isPhysicalChannelOpen)
    }

    @Test
    fun openPhysicalChannelSuccess() {
        presentMockTag()
        every { tagProxy?.isConnected } returns true
        reader.openPhysicalChannel()
    }

    @Test(expected = KeypleReaderException::class)
    fun openPhysicalChannelError() {
        // init
        presentMockTag()
        every { tagProxy?.isConnected } returns false
        every { tagProxy?.connect() } throws IOException()

        // test
        reader.openPhysicalChannel()
    }

    @Test
    fun closePhysicalChannelSuccess() {
        // init
        presentMockTag()
        every { tagProxy?.isConnected } returns true

        // test
        reader.closePhysicalChannel()
        // no exception
        Assert.assertTrue(true)
    }

    @Test(expected = KeypleReaderException::class)
    fun closePhysicalChannelError() {
        // init
        presentMockTag()
        every { tagProxy?.isConnected } returns true
        every { tagProxy?.close() } throws IOException()

        // test
        reader.closePhysicalChannel()
        // throw exception
    }

    // ---- TRANSMIT TEST ----------- //

    @Test
    fun transmitAPDUSuccess() {
        // init
        presentMockTag()
        val `in` = byteArrayOf(0x90.toByte(), 0x00)
        val out = byteArrayOf(0x90.toByte(), 0x00)
        every { tagProxy?.transceive(`in`) } returns out

        // test
        val outBB = reader.transmitApdu(`in`)

        // assert
        Assert.assertArrayEquals(out, outBB)
    }

    @Test(expected = KeypleReaderException::class)
    fun transmitAPDUError() {
        // init
        presentMockTag()
        val `in` = byteArrayOf(0x90.toByte(), 0x00)
        every { tagProxy?.transceive(`in`) } throws IOException()

        // test
        reader.transmitApdu(`in`)

        // throw exception
    }

    // TODO Replace this test by a getCurrentProtocol test
//    @Test
//    @Throws(KeypleException::class, IOException::class)
//    fun protocolFlagMatchesTrue() {
//        // init
//        presentMockTag()
//        reader.addCardProtocolSetting(CardCommonProtocols.PROTOCOL_ISO14443_4,
//                AndroidNfcProtocolSettings.getSetting(CardCommonProtocols.PROTOCOL_ISO14443_4))
//        every { tagProxy?.tech } returns AndroidNfcProtocolSettings.getSetting(CardCommonProtocols.PROTOCOL_ISO14443_4)
//
//        // test
//        Assert.assertTrue(reader.protocolFlagMatches(CardCommonProtocols.PROTOCOL_ISO14443_4))
//    }

    // ----- TEST PARAMETERS ------ //

    @Test
    @Throws(IllegalArgumentException::class)
    fun bCheckReaderParams() { // Must be ran in 2nd position as AndroidNfcReaderImpl is a singleton
        reader.noPlateformSound = true
        Assert.assertEquals(NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS, reader.flags)

        reader.noPlateformSound = false
        Assert.assertEquals(0, reader.flags)

        reader.skipNdefCheck = true
        Assert.assertEquals(NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, reader.flags)

        reader.skipNdefCheck = false
        Assert.assertEquals(0, reader.flags)

        reader.presenceCheckDelay = 10
        Assert.assertEquals(10, reader.options.get(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY))
    }

    @Test
    fun onTagReceivedException() {
        every { TagProxy.getTagProxy(tag) } throws KeypleReaderIOException("")
        reader.onTagDiscovered(tag) // Should not throw an exception
    }

    @Test
    fun waitForCardAbsentNative() {
        Assert.assertFalse(reader.waitForCardAbsentNative())
    }

    @Test
    fun stopWaitForCardRemoval() {
        reader.stopWaitForCardRemoval()
        Assert.assertTrue(true) // Previous call didn't throw any exception
    }
    // -------- helpers ---------- //

    private fun presentMockTag() {
        reader.onTagDiscovered(tag)
    }
}
