/*
 ***************************************************************************************************
 * Copyright (c) 2018 Calypso Networks Association https://www.calypsonet-asso.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information regarding copyright
 * ownership.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 **************************************************************************************************/
package org.eclipse.keyple.plugin.android.cone2;


import android.os.SystemClock;

import org.eclipse.keyple.core.seproxy.exception.KeypleIOReaderException;
import org.eclipse.keyple.core.seproxy.plugin.AbstractThreadedLocalReader;
import org.eclipse.keyple.core.seproxy.protocol.SeProtocol;
import org.eclipse.keyple.core.seproxy.protocol.TransmissionMode;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import fr.coppernic.sdk.ask.Defines;
import fr.coppernic.sdk.ask.EmvStatus;
import fr.coppernic.sdk.ask.Reader;
import fr.coppernic.sdk.ask.RfidTag;
import fr.coppernic.sdk.ask.sCARD_SearchExt;


/**
 * Implementation of {@link org.eclipse.keyple.core.seproxy.SeReader} to communicate with NFC Tag
 * using Coppernic C-One 2 device
 *
 */
public final class Cone2ContactlessReaderImpl extends AbstractThreadedLocalReader implements Cone2ContactlessReader {
    private final Map<String, String> parameters = new HashMap<String, String>();

    // ASK reader
    private Reader reader;
    // RFID tag information returned by startDiscovery
    private RfidTag rfidTag;
    // Indicates whether or not physical channel is opened.
    // Physical channel is irrelevant for ASK reader
    private AtomicBoolean isPhysicalChannelOpened = new AtomicBoolean(false);
    // This boolean indicates that a card has been discovered
    private AtomicBoolean isCardDiscovered = new AtomicBoolean(false);

    /**
     * Private constructor
     */
    Cone2ContactlessReaderImpl() {
        super(PLUGIN_NAME, READER_NAME);

        // We set parameters to default values
        parameters.put(CHECK_FOR_ABSENCE_TIMEOUT_KEY,
                CHECK_FOR_ABSENCE_TIMEOUT_DEFAULT);
        parameters.put(THREAD_WAIT_TIMEOUT_KEY,
                THREAD_WAIT_TIMEOUT_DEFAULT);

        setThreadWaitTimeout(Cone2ParametersUtils.getIntParam(parameters,
                THREAD_WAIT_TIMEOUT_KEY,
                THREAD_WAIT_TIMEOUT_DEFAULT));

        this.reader = Cone2AskReader.getInstance();
    }

    @Override
    protected boolean waitForCardPresent(long timeout) {
        long start = SystemClock.uptimeMillis();
        long end = SystemClock.uptimeMillis();
        while(end - start < timeout) {
            RfidTag rfidTag = enterHuntPhase();

            if (rfidTag.getCommunicationMode() != RfidTag.CommunicationMode.Unknown) {
                isCardDiscovered.set(true);
                this.rfidTag = rfidTag;
                return true;
            }

            end = SystemClock.uptimeMillis();
        }

        // At the end of the timeout, we simply stop the discovery if no card has been discovered.
        // If a card has been discovered, the discovery is automatically stopped.
        if (reader != null) {
            reader.stopDiscovery();
        }

        return false;
    }

    boolean logCardAbsent = false;

    @Override
    protected boolean waitForCardAbsent(long timeout) {
        SystemClock.sleep(1000);
        try {
            Cone2AskReader.acquireLock();

            EmvStatus emvStatus = new EmvStatus();
            int ret = reader.cscSendEmvCommand((byte)0x03, new byte[]{0x10}, emvStatus);
            if (ret != Defines.RCSC_Ok) {
                // TODO throw exception here
            }

            if(emvStatus.getStatus() == EmvStatus.Status.No_Picc_Found) {
                isCardDiscovered.set(false);
            }

        } finally {
            Cone2AskReader.releaseLock();
        }

        return !isCardDiscovered.get();
    }

    /**
     * Get Reader parameters
     *
     * @return parameters
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Sets parameters for ASK reader.
     *
     * @param key the parameter key
     * @param value the parameter value
     */
    @Override
    public void setParameter(String key, String value) throws IllegalArgumentException {
        if (parameters.containsKey(key)) {
            parameters.put(key, value);
        }
    }

    /**
     * The transmission mode is always CONTACTLESS
     * 
     * @return the current transmission mode
     */
    @Override
    public TransmissionMode getTransmissionMode() {
        return TransmissionMode.CONTACTLESS;
    }

    /**
     *
     * @return true if a SE is present
     */
    @Override
    protected boolean checkSePresence() {
        return isCardDiscovered.get();
    }

    @Override
    protected byte[] getATR() {
        if (rfidTag != null) {
            return rfidTag.getAtr();
        }

        return null;
    }

    @Override
    protected boolean isPhysicalChannelOpen() {
        return isPhysicalChannelOpened.get();
    }

    @Override
    protected void openPhysicalChannel() {
        isPhysicalChannelOpened.set(true);
    }

    @Override
    protected void closePhysicalChannel() {
        isPhysicalChannelOpened.set(false);
    }


    @Override
    protected byte[] transmitApdu(byte[] apduIn) throws KeypleIOReaderException {
        byte[] apduAnswer;

        try {
            Cone2AskReader.acquireLock();
            byte[] dataReceived = new byte[256];
            int[] dataReceivedLength = new int[1];

            if (reader != null) {
                reader.cscISOCommand(apduIn, apduIn.length, dataReceived, dataReceivedLength);
            } else {
                throw new KeypleIOReaderException("Reader has not been instantiated");
            }

            int length = dataReceivedLength[0];

            if (length < 2) {
                // Hopefully, this should not happen
                apduAnswer = new byte[2];
            } else {
                // first byte is always length value. We can ignore it
                apduAnswer = new byte[length-1];
                System.arraycopy(dataReceived, 1, apduAnswer, 0, apduAnswer.length);
            }
        } finally {
            Cone2AskReader.releaseLock();
        }

        return apduAnswer;
    }


    @Override
    protected boolean protocolFlagMatches(SeProtocol protocolFlag) {
        return true;
    }

    public void simulateCardRemoved() {
        logCardAbsent = false;
        isCardDiscovered.set(false);
    }

    public RfidTag enterHuntPhase() {
        // Thread synchronisation
        try {
            Cone2AskReader.acquireLock();
            // 1 - Sets the enter hunt phase parameters to no select application
            reader.cscEnterHuntPhaseParameters((byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00, new byte[]{}, (byte) 0x00, (byte) 0x00);
            sCARD_SearchExt search = new sCARD_SearchExt();
            search.OTH = 0;
            search.CONT = 0;
            search.INNO = 1;
            search.ISOA = 1;
            search.ISOB = 1;
            search.MIFARE = 0;
            search.MONO = 0;
            search.MV4k = 0;
            search.MV5k = 0;
            search.TICK = 0;
            int mask = Defines.SEARCH_MASK_INNO | Defines.SEARCH_MASK_ISOA | Defines.SEARCH_MASK_ISOB;

            byte[] com = new byte[1];
            int[] lpcbAtr = new int[1];
            byte[] atr = new byte[64];

            int ret = reader.cscSearchCardExt(search, mask, (byte) 0x00, (byte) 0x33, com, lpcbAtr, atr);

            RfidTag rfidTag;
            if (ret == Defines.RCSC_Timeout || com[0] == (byte) 0x6F || com[0] == (byte) 0x00) {
                isCardDiscovered.set(false);
                //isTransmitting.unlock();
                rfidTag = new RfidTag((byte) 0x6F, new byte[0]);
            } else {
                byte[] correctSizedAtr = new byte[lpcbAtr[0]];
                System.arraycopy(atr, 0, correctSizedAtr, 0, correctSizedAtr.length);
                rfidTag = new RfidTag(com[0], correctSizedAtr);
            }

            return rfidTag;
        } finally {
            Cone2AskReader.releaseLock();
        }
    }
}
