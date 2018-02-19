/*
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License version 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 */

package keyple.commands.utils;

import org.junit.Assert;
import org.junit.Test;
import org.keyple.calypso.commands.po.parser.GetDataFciRespPars;
import org.keyple.calypso.commands.po.parser.OpenSessionRespPars;
import org.keyple.calypso.commands.utils.ResponseUtils;

public class ResponseUtilsTest {

    @Test
    public void TestToFCI() {

        // Case if
        byte[] apduResponse = new byte[] {(byte) 0x6F, 0x22, (byte) 0x84, 0x08, 0x33, 0x4D, 0x54,
                0x52, 0x2E, 0x49, 0x43, 0x41, (byte) 0xA5, 0x16, (byte) 0xBF, 0x0C, 0x13,
                (byte) 0xC7, 0x08, 0x00, 0x00, 0x00, 0x00, 0x27, 0x4A, (byte) 0x9A, (byte) 0xB7,
                0x53, 0x07, 0x0A, 0x3C, 0x11, 0x32, 0x14, 0x10, 0x01};

        byte[] aid = new byte[] {0x33, 0x4D, 0x54, 0x52, 0x2E, 0x49, 0x43, 0x41};
        // AID aidExpected = new AID(aid);
        byte[] fciProprietaryTemplate = new byte[] {(byte) 0xBF, 0x0C, 0x13, (byte) 0xC7, 0x08,
                0x00, 0x00, 0x00, 0x00, 0x27, 0x4A, (byte) 0x9A, (byte) 0xB7, 0x53, 0x07, 0x0A,
                0x3C, 0x11, 0x32, 0x14, 0x10, 0x01};
        byte[] fciIssuerDiscretionaryData =
                new byte[] {(byte) 0xC7, 0x08, 0x00, 0x00, 0x00, 0x00, 0x27, 0x4A, (byte) 0x9A,
                        (byte) 0xB7, 0x53, 0x07, 0x0A, 0x3C, 0x11, 0x32, 0x14, 0x10, 0x01};
        byte[] applicationSN =
                new byte[] {0x00, 0x00, 0x00, 0x00, 0x27, 0x4A, (byte) 0x9A, (byte) 0xB7};
        GetDataFciRespPars.StartupInformation startupInfoExpected =
                new GetDataFciRespPars.StartupInformation((byte) 0x0A, (byte) 0x3C, (byte) 0x11,
                        (byte) 0x32, (byte) 0x14, (byte) 0x10, (byte) 0x01);

        GetDataFciRespPars.FCI fciExpected = new GetDataFciRespPars.FCI(aid, fciProprietaryTemplate,
                fciIssuerDiscretionaryData, applicationSN, startupInfoExpected);
        GetDataFciRespPars.FCI fciTested = GetDataFciRespPars.toFCI(apduResponse);

        Assert.assertArrayEquals(fciExpected.getApplicationSN(), fciTested.getApplicationSN());
        Assert.assertArrayEquals(fciExpected.getFciIssuerDiscretionaryData(),
                fciTested.getFciIssuerDiscretionaryData());
        Assert.assertArrayEquals(fciExpected.getFciProprietaryTemplate(),
                fciTested.getFciProprietaryTemplate());
        Assert.assertEquals(fciExpected.getStartupInformation().getApplicationSubtype(),
                fciTested.getStartupInformation().getApplicationSubtype());
        Assert.assertEquals(fciExpected.getStartupInformation().getApplicationType(),
                fciTested.getStartupInformation().getApplicationType());
        Assert.assertEquals(fciExpected.getStartupInformation().getBufferSize(),
                fciTested.getStartupInformation().getBufferSize());
        Assert.assertEquals(fciExpected.getStartupInformation().getPlatform(),
                fciTested.getStartupInformation().getPlatform());
        Assert.assertEquals(fciExpected.getStartupInformation().getSoftwareIssuer(),
                fciTested.getStartupInformation().getSoftwareIssuer());
        Assert.assertEquals(fciExpected.getStartupInformation().getSoftwareRevision(),
                fciTested.getStartupInformation().getSoftwareRevision());
        Assert.assertEquals(fciExpected.getStartupInformation().getSoftwareVersion(),
                fciTested.getStartupInformation().getSoftwareVersion());

        // Case else
        byte[] wrongApduResponse = new byte[] {(byte) 0x5F, 0x22, (byte) 0x84, 0x08, 0x33, 0x4D,
                0x54, 0x52, 0x2E, 0x49, 0x43, 0x41, (byte) 0xA5, 0x16, (byte) 0xBF, 0x0C, 0x13,
                (byte) 0xC7, 0x08, 0x00, 0x00, 0x00, 0x00, 0x27, 0x4A, (byte) 0x9A, (byte) 0xB7,
                0x53, 0x07, 0x0A, 0x3C, 0x11, 0x32, 0x14, 0x10, 0x01};

        fciTested = GetDataFciRespPars.toFCI(wrongApduResponse);

        Assert.assertNull(fciTested.getFciProprietaryTemplate());
        Assert.assertNull(fciTested.getFciIssuerDiscretionaryData());
        Assert.assertNull(fciTested.getApplicationSN());
        Assert.assertNull(fciTested.getStartupInformation());

        // Case if else
        wrongApduResponse = new byte[] {(byte) 0x6F, 0x22, (byte) 0x83, 0x08, 0x33, 0x4D, 0x54,
                0x52, 0x2E, 0x49, 0x43, 0x41, (byte) 0xA5, 0x16, (byte) 0xBF, 0x0C, 0x13,
                (byte) 0xC7, 0x08, 0x00, 0x00, 0x00, 0x00, 0x27, 0x4A, (byte) 0x9A, (byte) 0xB7,
                0x53, 0x07, 0x0A, 0x3C, 0x11, 0x32, 0x14, 0x10, 0x01};

        fciTested = GetDataFciRespPars.toFCI(wrongApduResponse);

        Assert.assertNotNull(fciTested.getFciProprietaryTemplate());
        Assert.assertNotNull(fciTested.getFciIssuerDiscretionaryData());
        Assert.assertNotNull(fciTested.getApplicationSN());
        Assert.assertNotNull(fciTested.getStartupInformation());

        // Case if else 2
        wrongApduResponse = new byte[] {(byte) 0x6F, 0x22, (byte) 0x84, 0x08, 0x33, 0x4D, 0x54,
                0x52, 0x2E, 0x49, 0x43, 0x41, (byte) 0xA6, 0x16, (byte) 0xBF, 0x0C, 0x13,
                (byte) 0xC7, 0x08, 0x00, 0x00, 0x00, 0x00, 0x27, 0x4A, (byte) 0x9A, (byte) 0xB7,
                0x53, 0x07, 0x0A, 0x3C, 0x11, 0x32, 0x14, 0x10, 0x01};

        fciTested = GetDataFciRespPars.toFCI(wrongApduResponse);

        Assert.assertNull(fciTested.getFciProprietaryTemplate());
        Assert.assertNotNull(fciTested.getFciIssuerDiscretionaryData());
        Assert.assertNotNull(fciTested.getApplicationSN());
        Assert.assertNotNull(fciTested.getStartupInformation());

        // Case if else 3
        wrongApduResponse = new byte[] {(byte) 0x6F, 0x22, (byte) 0x84, 0x08, 0x33, 0x4D, 0x54,
                0x52, 0x2E, 0x49, 0x43, 0x41, (byte) 0xA5, 0x16, (byte) 0xAF, 0x0C, 0x13,
                (byte) 0xC7, 0x08, 0x00, 0x00, 0x00, 0x00, 0x27, 0x4A, (byte) 0x9A, (byte) 0xB7,
                0x53, 0x07, 0x0A, 0x3C, 0x11, 0x32, 0x14, 0x10, 0x01};

        fciTested = GetDataFciRespPars.toFCI(wrongApduResponse);

        Assert.assertNotNull(fciTested.getFciProprietaryTemplate());
        Assert.assertNull(fciTested.getFciIssuerDiscretionaryData());
        Assert.assertNotNull(fciTested.getApplicationSN());
        Assert.assertNotNull(fciTested.getStartupInformation());

        // Case if else 3bis
        wrongApduResponse = new byte[] {(byte) 0x6F, 0x22, (byte) 0x84, 0x08, 0x33, 0x4D, 0x54,
                0x52, 0x2E, 0x49, 0x43, 0x41, (byte) 0xA5, 0x16, (byte) 0xBF, 0x0D, 0x13,
                (byte) 0xC7, 0x08, 0x00, 0x00, 0x00, 0x00, 0x27, 0x4A, (byte) 0x9A, (byte) 0xB7,
                0x53, 0x07, 0x0A, 0x3C, 0x11, 0x32, 0x14, 0x10, 0x01};

        fciTested = GetDataFciRespPars.toFCI(wrongApduResponse);

        Assert.assertNotNull(fciTested.getFciProprietaryTemplate());
        Assert.assertNull(fciTested.getFciIssuerDiscretionaryData());
        Assert.assertNotNull(fciTested.getApplicationSN());
        Assert.assertNotNull(fciTested.getStartupInformation());

        // Case if else 4
        wrongApduResponse = new byte[] {(byte) 0x6F, 0x22, (byte) 0x84, 0x08, 0x33, 0x4D, 0x54,
                0x52, 0x2E, 0x49, 0x43, 0x41, (byte) 0xA5, 0x16, (byte) 0xBF, 0x0C, 0x13,
                (byte) 0xC8, 0x08, 0x00, 0x00, 0x00, 0x00, 0x27, 0x4A, (byte) 0x9A, (byte) 0xB7,
                0x53, 0x07, 0x0A, 0x3C, 0x11, 0x32, 0x14, 0x10, 0x01};

        fciTested = GetDataFciRespPars.toFCI(wrongApduResponse);

        Assert.assertNotNull(fciTested.getFciProprietaryTemplate());
        Assert.assertNotNull(fciTested.getFciIssuerDiscretionaryData());
        Assert.assertNull(fciTested.getApplicationSN());
        Assert.assertNotNull(fciTested.getStartupInformation());

        // Case if else 4
        wrongApduResponse = new byte[] {(byte) 0x6F, 0x22, (byte) 0x84, 0x08, 0x33, 0x4D, 0x54,
                0x52, 0x2E, 0x49, 0x43, 0x41, (byte) 0xA5, 0x16, (byte) 0xBF, 0x0C, 0x13,
                (byte) 0xC7, 0x08, 0x00, 0x00, 0x00, 0x00, 0x27, 0x4A, (byte) 0x9A, (byte) 0xB7,
                0x54, 0x07, 0x0A, 0x3C, 0x11, 0x32, 0x14, 0x10, 0x01};

        fciTested = GetDataFciRespPars.toFCI(wrongApduResponse);

        Assert.assertNotNull(fciTested.getFciProprietaryTemplate());
        Assert.assertNotNull(fciTested.getFciIssuerDiscretionaryData());
        Assert.assertNotNull(fciTested.getApplicationSN());
        Assert.assertNull(fciTested.getStartupInformation());

    }

    @Test
    public void TestToSecureSession() {
        byte[] apduResponse = new byte[] {(byte) 0x8F, 0x05, 0x75, 0x1A, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x30, 0x7E, (byte) 0x1D, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00};

        byte[] transactionCounter = new byte[] {(byte) 0x8F, 0x05, 0x75};
        byte[] randomNumber = new byte[] {0x1A, 0x00, 0x00, 0x00, 0x00};
        byte kif = 0x00;
        byte kvc = (byte) 0x00;

        boolean isPreviousSessionRatifiedExpected = true;
        boolean isManageSecureSessionAuthorizedExpected = false;
        byte[] originalData = new byte[] {};

        OpenSessionRespPars.SecureSession SecureSessionExpected =
                new OpenSessionRespPars.SecureSession(transactionCounter, randomNumber,
                        isPreviousSessionRatifiedExpected, isManageSecureSessionAuthorizedExpected,
                        kif, kvc, originalData, apduResponse);
        OpenSessionRespPars.SecureSession SecureSessionTested =
                OpenSessionRespPars.toSecureSessionRev32(apduResponse);

        Assert.assertArrayEquals(SecureSessionExpected.getOriginalData(),
                SecureSessionTested.getOriginalData());
        Assert.assertArrayEquals(SecureSessionExpected.getSecureSessionData(),
                SecureSessionTested.getSecureSessionData());
        Assert.assertEquals(SecureSessionExpected.getKIF(), SecureSessionTested.getKIF());
        Assert.assertEquals(SecureSessionExpected.getKVC(), SecureSessionTested.getKVC());
        Assert.assertArrayEquals(SecureSessionExpected.getChallengeRandomNumber(),
                SecureSessionTested.getChallengeRandomNumber());
        Assert.assertArrayEquals(SecureSessionExpected.getChallengeTransactionCounter(),
                SecureSessionTested.getChallengeTransactionCounter());
    }

    @Test
    public void TestToSecureSessionRev2() {

        // Case Else
        byte[] apduResponse =
                new byte[] {(byte) 0x7E, (byte) 0x03, (byte) 0x0D, (byte) 0x14, (byte) 0x53};

        byte[] transactionCounter = new byte[] {(byte) 0x03, (byte) 0x0D, (byte) 0x14};
        byte[] randomNumber = new byte[] {(byte) 0x53};
        byte kvc = (byte) 0x7E;

        boolean isPreviousSessionRatifiedExpected = false;
        boolean isManageSecureSessionAuthorizedExpected = false;
        byte[] originalData = null;

        OpenSessionRespPars.SecureSession SecureSessionExpected =
                new OpenSessionRespPars.SecureSession(transactionCounter, randomNumber,
                        isPreviousSessionRatifiedExpected, isManageSecureSessionAuthorizedExpected,
                        kvc, originalData, apduResponse);
        OpenSessionRespPars.SecureSession SecureSessionTested =
                OpenSessionRespPars.toSecureSessionRev2(apduResponse);

        Assert.assertArrayEquals(SecureSessionExpected.getSecureSessionData(),
                SecureSessionTested.getSecureSessionData());
        Assert.assertEquals(SecureSessionExpected.getKVC(), SecureSessionTested.getKVC());
        Assert.assertArrayEquals(SecureSessionExpected.getChallengeRandomNumber(),
                SecureSessionTested.getChallengeRandomNumber());
        Assert.assertArrayEquals(SecureSessionExpected.getChallengeTransactionCounter(),
                SecureSessionTested.getChallengeTransactionCounter());

        // Case If Else
        byte[] apduResponseCaseTwo = new byte[] {(byte) 0x7E, (byte) 0x03, (byte) 0x0D, (byte) 0x14,
                (byte) 0x53, (byte) 0x30, 0x00, 0x04, 0x01, 0x02, 0x03, 0x04};
        byte[] originalDataCaseTwo = new byte[] {(byte) 0x7E, (byte) 0x03, (byte) 0x0D, (byte) 0x14,
                (byte) 0x53, (byte) 0xFF, 0x00, 0x04, 0x01, 0x02, 0x03, 0x04};

        OpenSessionRespPars.SecureSession SecureSessionExpectedCaseTwo =
                new OpenSessionRespPars.SecureSession(transactionCounter, randomNumber,
                        isPreviousSessionRatifiedExpected, isManageSecureSessionAuthorizedExpected,
                        kvc, originalDataCaseTwo, apduResponseCaseTwo);
        OpenSessionRespPars.SecureSession SecureSessionTestedCaseTwo =
                OpenSessionRespPars.toSecureSessionRev2(apduResponseCaseTwo);

        Assert.assertArrayEquals(SecureSessionExpectedCaseTwo.getSecureSessionData(),
                SecureSessionTestedCaseTwo.getSecureSessionData());
        Assert.assertEquals(SecureSessionExpectedCaseTwo.getKVC(),
                SecureSessionTestedCaseTwo.getKVC());
        Assert.assertArrayEquals(SecureSessionExpectedCaseTwo.getChallengeRandomNumber(),
                SecureSessionTestedCaseTwo.getChallengeRandomNumber());
        Assert.assertArrayEquals(SecureSessionExpectedCaseTwo.getChallengeTransactionCounter(),
                SecureSessionTestedCaseTwo.getChallengeTransactionCounter());

        // Case If If
        byte[] apduResponseCaseThree = new byte[] {(byte) 0x7E, (byte) 0x03, (byte) 0x0D,
                (byte) 0x14, (byte) 0x53, (byte) 0xFF, 0x00, 0x04, 0x01, 0x02, 0x03, 0x04};
        byte[] originalDataCaseThree = new byte[] {(byte) 0x7E, (byte) 0x03, (byte) 0x0D,
                (byte) 0x14, (byte) 0x53, (byte) 0xFF, 0x00, 0x04, 0x01, 0x02, 0x03, 0x04};

        OpenSessionRespPars.SecureSession SecureSessionExpectedCaseThree =
                new OpenSessionRespPars.SecureSession(transactionCounter, randomNumber,
                        isPreviousSessionRatifiedExpected, isManageSecureSessionAuthorizedExpected,
                        kvc, originalDataCaseThree, apduResponseCaseThree);
        OpenSessionRespPars.SecureSession SecureSessionTestedCaseThree =
                OpenSessionRespPars.toSecureSessionRev2(apduResponseCaseThree);

        Assert.assertArrayEquals(SecureSessionExpectedCaseThree.getSecureSessionData(),
                SecureSessionTestedCaseThree.getSecureSessionData());
        Assert.assertEquals(SecureSessionExpectedCaseThree.getKVC(),
                SecureSessionTestedCaseThree.getKVC());
        Assert.assertArrayEquals(SecureSessionExpectedCaseThree.getChallengeRandomNumber(),
                SecureSessionTestedCaseThree.getChallengeRandomNumber());
        Assert.assertArrayEquals(SecureSessionExpectedCaseThree.getChallengeTransactionCounter(),
                SecureSessionTestedCaseThree.getChallengeTransactionCounter());
    }

    @Test
    public void TestToKVCRev2() {
        byte[] apduResponse =
                new byte[] {(byte) 0x7E, (byte) 0x03, (byte) 0x0D, (byte) 0x14, (byte) 0x53};
        byte KVCRev2Expected = (byte) 0x7E;
        byte KVCRev2Tested = ResponseUtils.toKVCRev2(apduResponse);

        Assert.assertEquals(KVCRev2Expected, KVCRev2Tested);

        byte[] apduResponseCaseTwo =
                new byte[] {(byte) 0x7E, (byte) 0x03, (byte) 0x0D, (byte) 0x14, (byte) 0x53};

    }

}
