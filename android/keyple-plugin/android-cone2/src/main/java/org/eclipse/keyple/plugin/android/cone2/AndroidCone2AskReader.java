package org.eclipse.keyple.plugin.android.cone2;

import android.content.Context;

import java.lang.ref.WeakReference;

import fr.coppernic.sdk.ask.Defines;
import fr.coppernic.sdk.ask.Reader;
import fr.coppernic.sdk.utils.core.CpcDefinitions;
import fr.coppernic.sdk.utils.io.InstanceListener;

/**
 * This class provides the one and only Reader instance.
 * The purpose of provideing only one instance is to share the uniqueAskReaderInstance between contactless and
 * contact interfaces.
 */
public class AndroidCone2AskReader {
    // The unique reader instance for whole API
    private static WeakReference<Reader> uniqueAskReaderInstance;

    // Interface needed because the instantiation of the Reader instance is asynchronous.
    public interface ReaderListener {
        void onInstanceAvailable(Reader reader);
        void onError(int error);
    }

    /**
     * Provides the one and only instance of ASK reader
     * @param context A context
     * @param listener ReaderListener, needed because the instantiation is asynchronous
     */
    public static void getInstance(Context context, final ReaderListener listener) {
        // If uniqueAskReaderInstance is null, instantiates it
        if (uniqueAskReaderInstance.get() == null) {
            Reader.getInstance(context, new InstanceListener<Reader>() {
                @Override
                public void onCreated(Reader reader) {
                    // Stores the instance
                    AndroidCone2AskReader.uniqueAskReaderInstance = new WeakReference<Reader>(reader);
                    // Opens reader
                    AndroidCone2AskReader.uniqueAskReaderInstance.get().cscOpen(
                            CpcDefinitions.ASK_READER_PORT,
                            115200,
                            false);
                    // Initializes reader
                    StringBuilder sb = new StringBuilder();
                    int ret = AndroidCone2AskReader.uniqueAskReaderInstance.get().cscVersionCsc(sb);
                    if (ret == Defines.RCSC_Ok) {
                        listener.onInstanceAvailable(AndroidCone2AskReader
                                .uniqueAskReaderInstance
                                .get());
                    } else {
                        listener.onError(ret);
                    }
                }

                @Override
                public void onDisposed(Reader reader) {

                }
            });
        } else {
            // Or provides the current instance
            listener.onInstanceAvailable(uniqueAskReaderInstance.get());
        }
    }
}
