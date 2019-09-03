package org.eclipse.keyple.plugin.android.cone2;

import android.content.Context;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.eclipse.keyple.core.seproxy.exception.KeypleIOReaderException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import fr.coppernic.sdk.ask.Reader;
import fr.coppernic.sdk.power.impl.cone.ConePeripheral;
import fr.coppernic.sdk.utils.core.CpcBytes;
import fr.coppernic.sdk.utils.io.InstanceListener;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class AndroidCone2ReaderAndroidTestImpl {

    private Context context;
    private AndroidCone2ReaderImpl reader;

    @Before
    public void before() {
        // Context of the app under test.
        context = InstrumentationRegistry.getTargetContext();

        ConePeripheral.RFID_ASK_UCM108_GPIO.getDescriptor().power(context, true)
                .blockingGet();

        Reader.getInstance(context, new InstanceListener<Reader>() {
            @Override
            public void onCreated(Reader reader) {
                reader.cscOpen("/dev/ttyHSL1", 115200, false);
                reader.cscVersionCsc(new StringBuilder());
                AndroidCone2ReaderAndroidTestImpl.this.reader = AndroidCone2ReaderImpl.getInstance(reader);
                AndroidCone2ReaderAndroidTestImpl.this.reader.startPolling();
            }

            @Override
            public void onDisposed(Reader reader) {

            }
        });

    }

    @After
    public void after() {
        ConePeripheral.RFID_ASK_UCM108_GPIO.getDescriptor().power(context, false)
                .blockingGet();
    }

    @Test
    public void getAtrTest() {
        // Waits for the card to be detected
        // TODO Improve this part
        SystemClock.sleep(2000);

        // Checks ATR of the card
        byte[] atr = reader.getATR();
        Log.d("AndroidCone2ReaderAndroidTest", CpcBytes.byteArrayToString(atr));
        assertThat(atr, is(new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x26, 0x12, 0x11, 0x55, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00}));
    }

    @Test
    public void transmitApduTest() {
        // Waits for the card to be detected
        // TODO Improve this part
        SystemClock.sleep(2000);

        String apduString = "00A404000AA0000004040125090101";
        byte[] apdu = CpcBytes.parseHexStringToArray(apduString);
        try {
            byte[] answer = reader.transmitApdu(apdu);
            Log.d("AndroidCone2ReaderAndroidTest", CpcBytes.byteArrayToString(answer));

            String answerString = "2F6F2A8410A0000004040125090101000000000000A516BF0C13C708000000002612115553070628114210122B9000";
            byte[] expectedAnswer = CpcBytes.parseHexStringToArray(answerString);

            assertThat(answer, is(expectedAnswer));
        } catch (KeypleIOReaderException e) {
            e.printStackTrace();
        }
    }
}
