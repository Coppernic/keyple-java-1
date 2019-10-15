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
package org.eclipse.keyple.example.generic.pc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.EnumSet;
import org.eclipse.keyple.core.seproxy.SeProxyService;
import org.eclipse.keyple.core.seproxy.SeReader;
import org.eclipse.keyple.core.seproxy.event.ObservableReader;
import org.eclipse.keyple.core.seproxy.exception.KeypleBaseException;
import org.eclipse.keyple.core.seproxy.protocol.SeCommonProtocols;
import org.eclipse.keyple.example.generic.common.SeProtocolDetectionEngine;
import org.eclipse.keyple.plugin.pcsc.PcscPluginFactory;
import org.eclipse.keyple.plugin.pcsc.PcscProtocolSetting;
import org.eclipse.keyple.plugin.pcsc.PcscReader;

/**
 * This class handles the reader events generated by the SeProxyService
 */
public class Demo_SeProtocolDetection_Pcsc {

    public Demo_SeProtocolDetection_Pcsc() {
        super();
    }

    /**
     * Application entry
     *
     * @param args the program arguments
     * @throws IllegalArgumentException in case of a bad argument
     * @throws KeypleBaseException if a reader error occurs
     */
    public static void main(String[] args) throws IllegalArgumentException, KeypleBaseException {
        /* get the SeProxyService instance */
        SeProxyService seProxyService = SeProxyService.getInstance();

        /* Assign PcscPlugin to the SeProxyService */
        seProxyService.registerPlugin(new PcscPluginFactory());

        /* attempt to get the SeReader (the right reader should be ready here) */
        SeReader poReader =
                ReaderUtilities.getReaderByName(PcscReadersSettings.PO_READER_NAME_REGEX);

        if (poReader == null) {
            throw new IllegalStateException("Bad PO/SAM setup");
        }

        System.out.println("PO Reader  : " + poReader.getName());
        poReader.setParameter(PcscReader.SETTING_KEY_LOGGING, "true");

        /* create an observer class to handle the SE operations */
        SeProtocolDetectionEngine observer = new SeProtocolDetectionEngine();

        observer.setReader(poReader);

        /* configure reader */
        poReader.setParameter(PcscReader.SETTING_KEY_PROTOCOL, PcscReader.SETTING_PROTOCOL_T1);

        // Protocol detection settings.
        // add 8 expected protocols with three different methods:
        // - using a custom enumset
        // - adding protocols individually
        // A real application should use only one method.

        // Method 1
        // add several settings at once with setting an enumset
        poReader.setSeProtocolSetting(PcscProtocolSetting.getSpecificSettings(EnumSet.of(
                SeCommonProtocols.PROTOCOL_MIFARE_CLASSIC, SeCommonProtocols.PROTOCOL_MIFARE_UL)));

        // Method 2
        // append protocols individually
        // no change
        poReader.addSeProtocolSetting(SeCommonProtocols.PROTOCOL_MEMORY_ST25,
                PcscProtocolSetting.PCSC_PROTOCOL_SETTING
                        .get(SeCommonProtocols.PROTOCOL_MEMORY_ST25));

        // regex extended
        poReader.addSeProtocolSetting(SeCommonProtocols.PROTOCOL_ISO14443_4,
                PcscProtocolSetting.PCSC_PROTOCOL_SETTING.get(SeCommonProtocols.PROTOCOL_ISO14443_4)
                        + "|3B8D.*");

        // Set terminal as Observer of the first reader
        ((ObservableReader) poReader).addObserver(observer);

        // Set Default selection
        ((ObservableReader) poReader).setDefaultSelectionRequest(observer.prepareSeSelection(),
                ObservableReader.NotificationMode.ALWAYS, ObservableReader.PollingMode.CONTINUE);

        // wait for Enter key to exit.
        System.out.println("Press Enter to exit");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            int c = 0;
            try {
                c = br.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (c == 0x0A) {
                System.out.println("Exiting...");
                System.exit(0);
            }
        }
    }
}
