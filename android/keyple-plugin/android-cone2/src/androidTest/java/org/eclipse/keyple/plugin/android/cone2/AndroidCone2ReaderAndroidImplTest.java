package org.eclipse.keyple.plugin.android.cone2;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.eclipse.keyple.core.seproxy.exception.KeypleIOReaderException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import fr.coppernic.sdk.ask.Reader;
import fr.coppernic.sdk.power.impl.cone.ConePeripheral;
import fr.coppernic.sdk.utils.core.CpcBytes;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * These tests must be executed with a card present in front of the antenna of the contactless
 * reader
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class AndroidCone2ReaderAndroidImplTest extends TestBase {

    private Context context;
    private AndroidCone2ReaderImpl reader;

    @Before
    public void before() throws InterruptedException {
        // Context of the app under test.
        context = InstrumentationRegistry.getTargetContext();
        // Powers on contactless reader
        ConePeripheral.RFID_ASK_UCM108_GPIO.getDescriptor().power(context, true)
                .blockingGet();
        // Initializes the AndroidCone2AskReaderFactory unique instance
        AndroidCone2AskReaderFactory.getInstance(context, new AndroidCone2AskReaderFactory.ReaderListener() {
            @Override
            public void onInstanceAvailable(Reader reader) {
                // Reader has been initialized
                unblock();
            }

            @Override
            public void onError(int error) {

            }
        });
        //Waits for ASK reader object be instantiated and initialized
        block();
        // Reader can now be instantiated
        reader = new AndroidCone2ReaderImpl();
    }

    @After
    public void after() {
        // Switches RFID reader off
        ConePeripheral.RFID_ASK_UCM108_GPIO.getDescriptor().power(context, false)
                .blockingGet();
        // Clears instance to be able to initialize reader again
        AndroidCone2AskReaderFactory.clearInstance();
    }

    @Test
    public void waitForCardPresentTest() {
        // Waits for the card to be detected
        assertThat(AndroidCone2ReaderAndroidImplTest.this.reader.waitForCardPresent(2000), is(true));
    }

    @Test
    public void getAtrTest() {
        // Polls for card
        waitForCardPresentTest();
        // Checks ATR of the card
        byte[] atr = reader.getATR();
        assertThat(atr, is(new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x26, 0x12, 0x11, 0x55, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00}));
    }

    @Test
    public void transmitApduTest() throws KeypleIOReaderException {
        // Polls for card
        waitForCardPresentTest();
        // Creates APDU byte array
        String apduString = "00A404000AA0000004040125090101";
        byte[] apdu = CpcBytes.parseHexStringToArray(apduString);
        // Sends APDU to card
        byte[] answer = reader.transmitApdu(apdu);
        // Checks answer
        String answerString = "6F2A8410A0000004040125090101000000000000A516BF0C13C70800000000261" +
                "2115553070628114210122B9000";
        byte[] expectedAnswer = CpcBytes.parseHexStringToArray(answerString);
        assertThat(answer, is(expectedAnswer));
    }
}
