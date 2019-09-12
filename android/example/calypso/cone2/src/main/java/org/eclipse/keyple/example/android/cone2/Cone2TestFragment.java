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
package org.eclipse.keyple.example.android.cone2;



import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.TypefaceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import static org.eclipse.keyple.calypso.command.sam.SamRevision.AUTO;
import static org.eclipse.keyple.calypso.command.sam.SamRevision.S1D;
import org.eclipse.keyple.calypso.command.po.parser.ReadDataStructure;
import org.eclipse.keyple.calypso.command.po.parser.ReadRecordsRespPars;
import org.eclipse.keyple.calypso.transaction.CalypsoPo;
import org.eclipse.keyple.calypso.transaction.CalypsoSam;
import org.eclipse.keyple.calypso.transaction.PoResource;
import org.eclipse.keyple.calypso.transaction.PoSelectionRequest;
import org.eclipse.keyple.calypso.transaction.PoSelector;
import org.eclipse.keyple.calypso.transaction.PoTransaction;
import org.eclipse.keyple.calypso.transaction.SamResource;
import org.eclipse.keyple.calypso.transaction.SamSelectionRequest;
import org.eclipse.keyple.calypso.transaction.SamSelector;
import org.eclipse.keyple.calypso.transaction.SecuritySettings;
import org.eclipse.keyple.core.selection.MatchingSelection;
import org.eclipse.keyple.core.selection.SeSelection;
import org.eclipse.keyple.core.selection.SelectionsResult;
import org.eclipse.keyple.core.seproxy.ChannelState;
import org.eclipse.keyple.core.seproxy.SeProxyService;
import org.eclipse.keyple.core.seproxy.SeReader;
import org.eclipse.keyple.core.seproxy.SeSelector;
import org.eclipse.keyple.core.seproxy.event.AbstractDefaultSelectionsResponse;
import org.eclipse.keyple.core.seproxy.event.ObservableReader;
import org.eclipse.keyple.core.seproxy.event.ReaderEvent;
import org.eclipse.keyple.core.seproxy.exception.KeypleBaseException;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException;
import org.eclipse.keyple.core.seproxy.exception.NoStackTraceThrowable;
import org.eclipse.keyple.core.seproxy.protocol.SeCommonProtocols;
import org.eclipse.keyple.core.util.ByteArrayUtil;
import org.eclipse.keyple.plugin.android.cone2.Cone2AskReader;
import org.eclipse.keyple.plugin.android.cone2.Cone2AskReader.ReaderListener;
import org.eclipse.keyple.plugin.android.cone2.Cone2ContactlessReaderImpl;
import org.eclipse.keyple.plugin.android.cone2.Cone2Factory;
import org.eclipse.keyple.plugin.android.cone2.Cone2ContactlessReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.coppernic.sdk.ask.Reader;
import fr.coppernic.sdk.power.PowerManager;
import fr.coppernic.sdk.power.api.PowerListener;
import fr.coppernic.sdk.power.api.peripheral.Peripheral;
import fr.coppernic.sdk.power.impl.cone.ConePeripheral;
import fr.coppernic.sdk.utils.core.CpcResult;


/**
 * Test the Keyple NFC Plugin Configure the NFC seReader Configure the Observability Run test commands
 * when appropriate tag is detected.
 */
public class Cone2TestFragment extends Fragment implements ObservableReader.ReaderObserver {

    private static final Logger LOG = LoggerFactory.getLogger(Cone2TestFragment.class);

    private static final String TAG = Cone2TestFragment.class.getSimpleName();
    private static final String TAG_NFC_ANDROID_FRAGMENT =
            "org.eclipse.keyple.plugin.android.nfc.AndroidNfcFragment";

    // UI
    private TextView mText;

    private SeReader seReader;
    private SeReader samReader;
    private SamResource samResource;
    private SeSelection seSelection;
    private int readEnvironmentParserIndex;


    public static Cone2TestFragment newInstance() {
        return new Cone2TestFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        // Define UI components
        View view =
                inflater.inflate(org.eclipse.keyple.example.android.cone2.R.layout.fragment_nfc_test,
                        container, false);
        mText = view.findViewById(org.eclipse.keyple.example.android.cone2.R.id.text);
        mText.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                initTextView();
                return true;
            }
        });

        view.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((Cone2ContactlessReaderImpl)(seReader)).simulateCardRemoved();
            }
        });

        initTextView();

        return view;
    }

    private final PowerListener powerListener = new PowerListener() {
        @Override
        public void onPowerUp(CpcResult.RESULT res, Peripheral peripheral) {
            // [2] Get an instance of Reader class
            Cone2AskReader.getInstance(getActivity(),readerListener);
        }

        @Override
        public void onPowerDown(CpcResult.RESULT res, Peripheral peripheral) {

        }
    };

    private final ReaderListener readerListener = new ReaderListener() {
        @Override
        public void onInstanceAvailable(Reader reader) {
            // [3] We can create a plugin factory
            Cone2Factory pluginFactory = new Cone2Factory();

            // [4] And then start keyple
            try {
                createPlugin(pluginFactory);
            } catch (NoStackTraceThrowable noStackTraceThrowable) {
                noStackTraceThrowable.printStackTrace();
            } catch (KeypleReaderException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onError(int i) {

        }
    };

    @Override
    public void onStart() {
        super.onStart();

        PowerManager.get().registerListener(powerListener);

        // [1] Power on ASK reader
        ConePeripheral.RFID_ASK_UCM108_GPIO.on(getContext());
    }

    @Override
    public void onStop() {
        super.onStop();

        // TODO close Reader and plugin properly before powering off
        ConePeripheral.RFID_ASK_UCM108_GPIO.off(getContext());

        LOG.debug("Remove task as an observer for ReaderEvents");
        ((ObservableReader) seReader).removeObserver(this);

        // destroy AndroidNFC fragment
        FragmentManager fm = getFragmentManager();
        Fragment f = fm.findFragmentByTag(TAG_NFC_ANDROID_FRAGMENT);
        if (f != null) {
            fm.beginTransaction().remove(f).commit();
        }
    }

    /**
     * Catch @{@link Cone2ContactlessReader} events When a SE is inserted, launch test commands
     **
     * @param event
     */
    @Override
    public void update(final ReaderEvent event) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                LOG.info("New ReaderEvent received : " + event.toString());

                switch (event.getEventType()) {
                    case SE_MATCHED:
                        runCalyspoTransaction(event.getDefaultSelectionsResponse());
                        break;

                    case SE_INSERTED:

                        mText.append("\n ---- \n");
                        mText.setText("Card inserted");
                        runCalyspoTransaction(event.getDefaultSelectionsResponse());
                        break;

                    case SE_REMOVAL:
                        initTextView();
                        break;

                    case IO_ERROR:
                        mText.append("\n ---- \n");
                        mText.setText("Error reading card");
                        break;

                }
            }
        });
    }


    private void createPlugin(Cone2Factory pluginFactory) throws NoStackTraceThrowable, KeypleReaderException {
        SeProxyService seProxyService = SeProxyService.getInstance();
        seProxyService.registerPlugin(pluginFactory);

        /*
         * Get a PO reader ready to work with Calypso PO. Use the getReader helper method from the
         * CalypsoUtilities class.
         */
        try {
            seReader = seProxyService.getPlugins().first().getReaders().last();
            ((ObservableReader) seReader).addObserver(Cone2TestFragment.this);
        } catch (KeypleReaderException e) {
            e.printStackTrace();
        }

        /*
         * Get a SAM reader ready to work with Calypso PO. Use the getReader helper method from the
         * CalypsoUtilities class.
         */

        try {
            samReader = seProxyService.getPlugins().first().getReaders().first();
        } catch (KeypleReaderException e) {
            e.printStackTrace();
        }

        /*
         * check the availability of the SAM doing a ATR based selection, open its physical and
         * logical channels and keep it open
         */
        SeSelection samSelection = new SeSelection();

        SamSelector samSelector = new SamSelector(S1D, ".*", "Selection SAM D6");

        /* Prepare selector, ignore AbstractMatchingSe here */
        samSelection.prepareSelection(new SamSelectionRequest(samSelector, ChannelState.KEEP_OPEN));
        CalypsoSam calypsoSam;

        try {
            calypsoSam = (CalypsoSam) samSelection.processExplicitSelection(samReader)
                    .getActiveSelection().getMatchingSe();
            if (!calypsoSam.isSelected()) {
                throw new IllegalStateException("Unable to open a logical channel for SAM!");
            } else {
            }
        } catch (KeypleReaderException e) {
            throw new IllegalStateException("Reader exception: " + e.getMessage());
        }

        samResource = new SamResource(samReader, calypsoSam);

        /* Check if the readers exists */
        if (seReader == null) {
            throw new IllegalStateException("Bad PO");
        }
    }

    /**
     * Run Calypso simple read transaction
     * 
     * @param defaultSelectionsResponse
     * 
     */
    private void runCalyspoTransaction(
            final AbstractDefaultSelectionsResponse defaultSelectionsResponse) {
        LOG.debug("Running Calypso Simple Read transaction");
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    initTextView();

                    /*
                     * print tag info in View
                     */
                    mText.append("Card detected");
                    mText.append("\n ---- \n");
                    mText.append("");
                    mText.append("\n ---- \n");
                    mText.append("UseCase Calypso #4: Po Authentication\n");
                    mText.append("PO Reader: " + seReader.getName() + "\n");
                    mText.append("SAM Reader: " + samResource.getSeReader().getName() + "\n");

                    /* Check if a PO is present in the reader */
                    if (seReader.isSePresent()) {

                        mText.append(" ---- \n");
                        mText.append("1st PO exchange:\n");
                        mText.append("AID based selection with reading of Environment file.\n");
                        mText.append(" ---- \n");

                        /*
                         * Prepare a Calypso PO selection
                         */
                        SeSelection seSelection = new SeSelection();

                        /*
                         * Setting of an AID based selection of a Calypso REV3 PO
                         *
                         * Select the first application matching the selection AID whatever the SE communication
                         * protocol keep the logical channel open after the selection
                         */

                        /*
                         * Calypso selection: configures a PoSelectionRequest with all the desired attributes to
                         * make the selection and read additional information afterwards
                         */
                        PoSelectionRequest poSelectionRequest = new PoSelectionRequest(
                                new PoSelector(SeCommonProtocols.PROTOCOL_ISO14443_4, null,
                                        new PoSelector.PoAidSelector(
                                                new SeSelector.AidSelector.IsoAid(CalypsoClassicInfo.AID),
                                                PoSelector.InvalidatedPo.REJECT),
                                        "AID: " + CalypsoClassicInfo.AID),
                                ChannelState.KEEP_OPEN);

                        /*
                         * Add the selection case to the current selection (we could have added other cases
                         * here)
                         */
                        seSelection.prepareSelection(poSelectionRequest);

                        /*
                         * Actual PO communication: operate through a single request the Calypso PO selection
                         * and the file read
                         */
                        SelectionsResult selectionsResult = seSelection.processExplicitSelection(seReader);

                        if (selectionsResult.hasActiveSelection()) {
                            MatchingSelection matchingSelection = selectionsResult.getActiveSelection();

                            CalypsoPo calypsoPo = (CalypsoPo) matchingSelection.getMatchingSe();
                            mText.append("The selection of the PO has succeeded.\n");

                            /* Go on with the reading of the first record of the EventLog file */
                            mText.append(" ---- \n");
                            mText.append("2nd PO exchange: \n");
                            mText.append("open and close a secure session to perform authentication.\n");
                            mText.append(" ---- \n");

                            PoTransaction poTransaction = new PoTransaction(new PoResource(seReader, calypsoPo),
                                    samResource, new SecuritySettings());

                            /*
                             * Prepare the reading order and keep the associated parser for later use once the
                             * transaction has been processed.
                             */
                            int readEventLogParserIndex = poTransaction.prepareReadRecordsCmd(
                                    CalypsoClassicInfo.SFI_EventLog, ReadDataStructure.SINGLE_RECORD_DATA,
                                    CalypsoClassicInfo.RECORD_NUMBER_1,
                                    String.format("EventLog (SFI=%02X, recnbr=%d))",
                                            CalypsoClassicInfo.SFI_EventLog,
                                            CalypsoClassicInfo.RECORD_NUMBER_1));

                            /*
                             * Open Session for the debit key
                             */
                            boolean poProcessStatus = poTransaction.processOpening(
                                    PoTransaction.ModificationMode.ATOMIC,
                                    PoTransaction.SessionAccessLevel.SESSION_LVL_DEBIT, (byte) 0, (byte) 0);

                            if (!poProcessStatus) {
                                throw new IllegalStateException("processingOpening failure.\n");
                            }

                            if (!poTransaction.wasRatified()) {
                                appendColoredText(mText,
                                        "Previous Secure Session was not ratified.\n",
                                        Color.RED);
                            }
                            /*
                             * Prepare the reading order and keep the associated parser for later use once the
                             * transaction has been processed.
                             */
                            int readEventLogParserIndexBis = poTransaction.prepareReadRecordsCmd(
                                    CalypsoClassicInfo.SFI_EventLog, ReadDataStructure.SINGLE_RECORD_DATA,
                                    CalypsoClassicInfo.RECORD_NUMBER_1,
                                    String.format("EventLog (SFI=%02X, recnbr=%d))",
                                            CalypsoClassicInfo.SFI_EventLog,
                                            CalypsoClassicInfo.RECORD_NUMBER_1));

                            poProcessStatus = poTransaction.processPoCommandsInSession();

                            /*
                             * Retrieve the data read from the parser updated during the transaction process
                             */
                            byte eventLog[] = (((ReadRecordsRespPars) poTransaction
                                    .getResponseParser(readEventLogParserIndexBis)).getRecords())
                                    .get((int) CalypsoClassicInfo.RECORD_NUMBER_1);

                            /* Log the result */
                            mText.append("EventLog file data: " + ByteArrayUtil.toHex(eventLog) + "\n");

                            if (!poProcessStatus) {
                                throw new IllegalStateException("processPoCommandsInSession failure.\n");
                            }

                            /*
                             * Close the Secure Session.
                             */
                            if (LOG.isInfoEnabled()) {
                                mText.append("PO Calypso session: Closing\n");
                            }

                            /*
                             * A ratification command will be sent (CONTACTLESS_MODE).
                             */
                            poProcessStatus = poTransaction.processClosing(ChannelState.CLOSE_AFTER);

                            if (!poProcessStatus) {
                                throw new IllegalStateException("processClosing failure.\n");
                            }

                            mText.append(" ---- \n");
                            mText.append("End of the Calypso PO processing.\n");
                            mText.append(" ---- \n");
                        } else {
                            appendColoredText(mText,
                                    "The selection of the PO has failed.",
                                    Color.RED);
                        }
                    } else {
                        appendColoredText(mText,
                                "No PO were detected.",
                                Color.RED);
                    }
//                    SelectionsResult selectionsResult =
//                            seSelection.processDefaultSelection(defaultSelectionsResponse);
//                    if (selectionsResult.hasActiveSelection()) {
//                        CalypsoPo calypsoPo =
//                                (CalypsoPo) selectionsResult.getActiveSelection().getMatchingSe();
//
//                        mText.append("\nCalypso PO selection: ");
//                        appendColoredText(mText, "SUCCESS\n", Color.GREEN);
//                        mText.append("AID: ");
//                        appendHexBuffer(mText, ByteArrayUtil.fromHex(CalypsoClassicInfo.AID));
//
//                        /*
//                         * Retrieve the data read from the parser updated during the selection
//                         * process
//                         */
//                        ReadRecordsRespPars readEnvironmentParser =
//                                (ReadRecordsRespPars) selectionsResult.getActiveSelection()
//                                        .getResponseParser(readEnvironmentParserIndex);
//
//                        byte environmentAndHolder[] = (readEnvironmentParser.getRecords())
//                                .get((int) CalypsoClassicInfo.RECORD_NUMBER_1);
//
//                        mText.append("\n\nEnvironment and Holder file: ");
//                        appendHexBuffer(mText, environmentAndHolder);
//
//                        appendColoredText(mText, "\n\n2nd PO exchange:\n", Color.BLACK);
//                        mText.append("* read the event log file");
//                        PoTransaction poTransaction =
//                                new PoTransaction(new PoResource(seReader, calypsoPo));
//
//                        /*
//                         * Prepare the reading order and keep the associated parser for later use
//                         * once the transaction has been processed.
//                         */
//                        int readEventLogParserIndex =
//                                poTransaction.prepareReadRecordsCmd(CalypsoClassicInfo.SFI_EventLog,
//                                        ReadDataStructure.SINGLE_RECORD_DATA,
//                                        CalypsoClassicInfo.RECORD_NUMBER_1,
//                                        String.format("EventLog (SFI=%02X, recnbr=%d))",
//                                                CalypsoClassicInfo.SFI_EventLog,
//                                                CalypsoClassicInfo.RECORD_NUMBER_1));
//
//                        /*
//                         * Actual PO communication: send the prepared read order, then close the
//                         * channel with the PO
//                         */
//                        if (poTransaction.processPoCommands(ChannelState.CLOSE_AFTER)) {
//                            mText.append("\nTransaction: ");
//                            appendColoredText(mText, "SUCCESS\n", Color.GREEN);
//
//                            /*
//                             * Retrieve the data read from the parser updated during the transaction
//                             * process
//                             */
//
//                            ReadRecordsRespPars readEventLogParser =
//                                    (ReadRecordsRespPars) poTransaction
//                                            .getResponseParser(readEventLogParserIndex);
//                            byte eventLog[] = (readEventLogParser.getRecords())
//                                    .get((int) CalypsoClassicInfo.RECORD_NUMBER_1);
//
//                            /* Log the result */
//                            mText.append("\nEventLog file:\n");
//                            appendHexBuffer(mText, eventLog);
//                        }
//                        appendColoredText(mText, "\n\nEnd of the Calypso PO processing.",
//                                Color.BLACK);
//                    } else {
//                        appendColoredText(mText,
//                                "The selection of the PO has failed. Should not have occurred due to the MATCHED_ONLY selection mode.",
//                                Color.RED);
//                    }
                } catch (KeypleReaderException e1) {
                    e1.fillInStackTrace();
                    appendColoredText(mText, "\nException: " + e1.getMessage(), Color.RED);
                } catch (Exception e) {
                    LOG.debug("Exception: " + e.getMessage());
                    appendColoredText(mText, "\nException: " + e.getMessage(), Color.RED);
                    e.fillInStackTrace();
                } catch (NoStackTraceThrowable noStackTraceThrowable) {
                    noStackTraceThrowable.printStackTrace();
                }
            }
                    

        });

    }



    /**
     * Revocation of the Activity from @{@link Cone2ContactlessReader} list of observers
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    /**
     * Initialize display
     */
    private void initTextView() {
        mText.setText("");// reset
        appendColoredText(mText, "Waiting for a smartcard...", Color.BLUE);
        mText.append("\n ---- \n");
    }

    /**
     * Append to tv a string containing an hex representation of the byte array provided in
     * argument.
     * <p>
     * The font used is monospaced.
     * 
     * @param tv TextView
     * @param ba byte array
     */
    private static void appendHexBuffer(TextView tv, byte[] ba) {
        int start = tv.getText().length();
        tv.append(ByteArrayUtil.toHex(ba));
        int end = tv.getText().length();

        Spannable spannableText = (Spannable) tv.getText();

        spannableText.setSpan(new TypefaceSpan("monospace"), start, end, 0);
        spannableText.setSpan(new RelativeSizeSpan(0.70f), start, end, 0);
    }

    /**
     * Append to tv a text colored according to the provided argument
     * 
     * @param tv TextView
     * @param text string
     * @param color color value
     */
    private static void appendColoredText(TextView tv, String text, int color) {
        int start = tv.getText().length();
        tv.append(text);
        int end = tv.getText().length();

        Spannable spannableText = (Spannable) tv.getText();
        spannableText.setSpan(new ForegroundColorSpan(color), start, end, 0);
    }
}
