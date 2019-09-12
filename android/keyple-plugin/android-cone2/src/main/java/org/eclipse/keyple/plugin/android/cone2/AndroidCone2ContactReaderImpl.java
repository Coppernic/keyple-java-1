package org.eclipse.keyple.plugin.android.cone2;

import org.eclipse.keyple.core.seproxy.SeSelector;
import org.eclipse.keyple.core.seproxy.exception.KeypleApplicationSelectionException;
import org.eclipse.keyple.core.seproxy.exception.KeypleBaseException;
import org.eclipse.keyple.core.seproxy.exception.KeypleChannelStateException;
import org.eclipse.keyple.core.seproxy.exception.KeypleIOReaderException;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException;
import org.eclipse.keyple.core.seproxy.exception.NoStackTraceThrowable;
import org.eclipse.keyple.core.seproxy.message.ApduResponse;
import org.eclipse.keyple.core.seproxy.plugin.AbstractStaticReader;
import org.eclipse.keyple.core.seproxy.protocol.SeProtocol;
import org.eclipse.keyple.core.seproxy.protocol.TransmissionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import fr.coppernic.sdk.ask.Reader;

public class AndroidCone2ContactReaderImpl extends AbstractStaticReader implements Cone2ContactReader {

    private static final Logger LOG = LoggerFactory.getLogger(AndroidCone2ReaderImpl.class);

    private static final String READER_NAME = "AndroidCone2ContactReader";
    private static final String PLUGIN_NAME = "AndroidCone2Plugin";

    private final Map<String, String> parameters = new HashMap<String, String>();

    private Reader reader;

    protected AndroidCone2ContactReaderImpl() {
        super(PLUGIN_NAME, READER_NAME);

        // We set parameters to default values
        // By default, contact reader is set tom SAM 1
        parameters.put(CONTACT_INTERFACE_ID,
                CONTACT_INTERFACE_ID_SAM_1);

        reader = AndroidCone2AskReaderFactory.getInstance();
    }

    @Override
    protected boolean checkSePresence() throws NoStackTraceThrowable {
        return false;
    }

    @Override
    protected byte[] getATR() {
        return new byte[0];
    }

    @Override
    protected ApduResponse openChannelForAid(SeSelector.AidSelector aidSelector) throws KeypleIOReaderException, KeypleChannelStateException, KeypleApplicationSelectionException {
        return null;
    }

    @Override
    protected void openPhysicalChannel() throws KeypleChannelStateException {

    }

    @Override
    protected void closePhysicalChannel() throws KeypleChannelStateException {

    }

    @Override
    protected boolean isPhysicalChannelOpen() {
        return false;
    }

    @Override
    protected boolean protocolFlagMatches(SeProtocol protocolFlag) throws KeypleReaderException {
        return false;
    }

    @Override
    protected byte[] transmitApdu(byte[] apduIn) throws KeypleIOReaderException {
        return new byte[0];
    }

    @Override
    public TransmissionMode getTransmissionMode() {
        return null;
    }

    @Override
    public Map<String, String> getParameters() {
        return null;
    }

    @Override
    public void setParameter(String key, String value) {
        if (parameters.containsKey(key)) {
            parameters.put(key, value);
        }
    }
}
