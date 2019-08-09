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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import fr.coppernic.sdk.ask.Defines;
import fr.coppernic.sdk.ask.Reader;
import fr.coppernic.sdk.ask.ReaderListener;
import fr.coppernic.sdk.ask.RfidTag;
import fr.coppernic.sdk.ask.SearchParameters;
import fr.coppernic.sdk.ask.sCARD_SearchExt;


/**
 * Implementation of {@link org.eclipse.keyple.core.seproxy.SeReader} to communicate with NFC Tag though
 * Coppernic C-One 2 device
 *
 */
public final class AndroidCone2Reader extends AbstractThreadedLocalReader {

    private static final Logger LOG = LoggerFactory.getLogger(AndroidCone2Reader.class);

    private static final String READER_NAME = "AndroidCone2Reader";
    private static final String PLUGIN_NAME = "AndroidCone2Plugin";

    private final Map<String, String> parameters = new HashMap<String, String>();

    // ASK reader
    private Reader reader;
    // RFID tag information returned by startDiscovery
    private RfidTag rfidTag;
    // Indicates whether or not physical channel is opened.
    // Physical channel is irrelevant for ASK reader
    private AtomicBoolean isPhysicalChannelOpened = new AtomicBoolean(false);
    // This variable assures that we are not checking for card when it is transmitting.
    // The reason is that the ASK reader is working synchronously and the reader logic is
    // asynchronous. If a command has been sent to the reader using transmitApdu, and then before
    // the answer a checkSePresence call is made, the reader will fall in timeout causing the API to
    // interpret this as a card removed event.
    private ReentrantLock isTransmitting = new ReentrantLock();
    // This boolean indicates that a card has been discovered
    private AtomicBoolean isCardDiscovered = new AtomicBoolean(false);

    /**
     * Private constructor
     */
    private AndroidCone2Reader() {
        super(PLUGIN_NAME, READER_NAME);

        // We set parameters to default values
        parameters.put(AndroidCone2Parameters.CHECK_FOR_ABSENCE_TIMEOUT_KEY,
                AndroidCone2Parameters.CHECK_FOR_ABSENCE_TIMEOUT_DEFAULT);
        parameters.put(AndroidCone2Parameters.THREAD_WAIT_TIMEOUT_KEY,
                AndroidCone2Parameters.THREAD_WAIT_TIMEOUT_DEFAULT);
        parameters.put(AndroidCone2Parameters.ACTIVE_READER_KEY,
                AndroidCone2Parameters.ACTIVE_READER_DEFAULT);

        setThreadWaitTimeout(AndroidCone2Parameters.getIntParam(parameters,
                AndroidCone2Parameters.THREAD_WAIT_TIMEOUT_KEY,
                AndroidCone2Parameters.THREAD_WAIT_TIMEOUT_DEFAULT));

        this.reader = AndroidCone2AskReader.getInstance();
    }

    private static AndroidCone2Reader instance;

    /**
     * Access point for the unique instance of singleton
     */
    static AndroidCone2Reader getInstance() {
        if (instance == null) {
            instance = new AndroidCone2Reader();
        }

        return instance;
    }

    @Override
    protected boolean waitForCardPresent(long timeout) {
        LOG.debug("waitForCardPresent");
        // Starts searchnig cards. The discovery is infinite and asynchronous, so we just start it
        // and wait for a card to be placed in the field.
        startPolling();

        long start = SystemClock.uptimeMillis();
        long end = SystemClock.uptimeMillis();
        while(end - start < timeout) {
            if (isCardDiscovered.get()) {
                return true;
            }
            end = SystemClock.uptimeMillis();
        }

        // At the end of the timeout, we simply stop the discovery if no card has been discovered.
        // If a card has been discovered, the discovery is automatically stopped.
        if (reader != null) {
            LOG.debug("Stops polling");
            reader.stopDiscovery();
        }

        return false;
    }

    @Override
    protected boolean waitForCardAbsent(long timeout) {
        // This method sends a neutral APDU, which will return RCSC_Timeout if no card is present in
        // the field. In case a card is in the field, it will return RCSC_Ok with error status.
        try {
            // Thread synchronisation
            isTransmitting.lock();
            LOG.debug("not transmitting");
            byte[] neutralApdu = {0x00, (byte) 0xA4, 0x00, 0x00, 0x00};
            byte[] bufOut = new byte[256];
            int[] lnOut = new int[1];
            // Changes timing of timeouts, by default timeout is 5000ms. A 5000ms would generate a
            // too long wait if the card is removed a the beginning of the call.
            reader.cscSetTimings(AndroidCone2Parameters.getIntParam(parameters,
                    AndroidCone2Parameters.CHECK_FOR_ABSENCE_TIMEOUT_KEY,
                    AndroidCone2Parameters.THREAD_WAIT_TIMEOUT_DEFAULT),
                    3000,
                    0);
            // Sends the neutral APDU and wait for answer
            int ret = reader.cscISOCommand(neutralApdu, neutralApdu.length, bufOut, lnOut);
            // Changes back the timeout to previous value
            reader.cscSetTimings(AndroidCone2Parameters.getIntParam(parameters,
                    AndroidCone2Parameters.FUNCTION_TIMEOUT_KEY,
                    AndroidCone2Parameters.FUNCTION_TIMEOUT_DEFAULT),
                    3000,
                    0);
            // A timeout has occurred, the card has been removed
            if (ret == Defines.RCSC_Timeout) {
                LOG.debug("Card removed");
                isCardDiscovered.set(false);
                //isTransmitting.unlock();
                return true;
            }
        } finally {
            isTransmitting.unlock();
        }

        // Either the API is transmitting or the return value of cscISOCommand is not null, so the
        // card is present
        return false;
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
        LOG.debug("transmitApdu");
        byte[] apduAnswer;

        try {
            isTransmitting.lock();
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
                LOG.error("Error, empty answer");
            } else {
                apduAnswer = new byte[length];
                System.arraycopy(dataReceived, 0, apduAnswer, 0, apduAnswer.length);
            }
        } finally {
            isTransmitting.unlock();
        }
        LOG.debug("End transmission");

        return apduAnswer;
    }


    @Override
    protected boolean protocolFlagMatches(SeProtocol protocolFlag) {
        return true;
    }

    /**
     * Starts polling for cards
     */
    void startPolling() {
        // TODO Add parameters
        // Sets the card detection
        sCARD_SearchExt search = new sCARD_SearchExt();
        search.OTH = 0;
        search.CONT = 0;
        search.INNO = 1;
        search.ISOA = 1;
        search.ISOB = 1;
        search.MIFARE = 1;
        search.MONO = 0;
        search.MV4k = 0;
        search.MV5k = 0;
        search.TICK = 0;
        int mask = Defines.SEARCH_MASK_INNO | Defines.SEARCH_MASK_ISOA | Defines.SEARCH_MASK_ISOB |
                Defines.SEARCH_MASK_MIFARE | Defines.SEARCH_MASK_MONO | Defines.SEARCH_MASK_MV4K |
                Defines.SEARCH_MASK_MV5K | Defines.SEARCH_MASK_TICK | Defines.SEARCH_MASK_OTH;
        SearchParameters parameters = new SearchParameters(search, mask, (byte) 0x01, (byte) 0x00);
        // Starts card detection
        if (reader != null) {
            reader.startDiscovery(parameters, new ReaderListener() {
                @Override
                public void onTagDiscovered(RfidTag rfidTag) {
                    LOG.debug("Tag discovered");
                    AndroidCone2Reader.this.rfidTag = rfidTag;
                    isCardDiscovered.set(true);
                }

                @Override
                public void onDiscoveryStopped() {
                    LOG.debug("Discovery stopped");
                }
            });
        }
    }
}
