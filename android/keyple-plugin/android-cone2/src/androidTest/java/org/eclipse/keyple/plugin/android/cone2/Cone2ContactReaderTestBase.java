package org.eclipse.keyple.plugin.android.cone2;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;

import fr.coppernic.sdk.ask.Reader;
import fr.coppernic.sdk.power.api.peripheral.Peripheral;
import fr.coppernic.sdk.power.impl.access.AccessPeripheral;
import fr.coppernic.sdk.power.impl.cone.ConePeripheral;
import fr.coppernic.sdk.utils.helpers.OsHelper;
import timber.log.Timber;

public class Cone2ContactReaderTestBase extends TestBase {
    private Context context;
    protected Cone2ContactReaderImpl reader;

    @Before
    public void before() throws InterruptedException {
        //Timber.plant(new Timber.DebugTree());
        // Context of the app under test.
        context = InstrumentationRegistry.getInstrumentation().getContext();

        Peripheral p = null;
        if (OsHelper.isCone()) {
            p = ConePeripheral.RFID_ASK_UCM108_GPIO;
        } else if (OsHelper.isAccess()){
            p = AccessPeripheral.RFID_ASK_UCM108_GPIO;
        }
        if (p!= null) {
            // Powers on contactless reader
            ConePeripheral.RFID_ASK_UCM108_GPIO.getDescriptor().power(context, true).blockingGet();
            // Initializes the Cone2AskReader unique instance
            Cone2AskReader.getInstance(context, new Cone2AskReader.ReaderListener() {
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
            // Contact reader can now be instantiated
            reader = new Cone2ContactReaderImpl();
        }
    }

    @After
    public void after() {
        // Switches RFID reader off
        ConePeripheral.RFID_ASK_UCM108_GPIO.getDescriptor().power(context, false)
                .blockingGet();
        // Clears instance to be able to initialize reader again
        Cone2AskReader.clearInstance();
    }
}
