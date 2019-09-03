package org.eclipse.keyple.plugin.android.cone2;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;

import fr.coppernic.sdk.ask.Defines;
import fr.coppernic.sdk.ask.Reader;
import fr.coppernic.sdk.utils.io.InstanceListener;

/**
 * This class provides the one and only Reader instance.
 * The purpose of provideing only one instance is to share the uniqueAskReaderInstance between contactless and
 * contact interfaces.
 */
public class AndroidCone2AskReaderFactory {
    // The unique reader instance for whole API
    private static WeakReference<Reader> uniqueAskReaderInstance = new WeakReference<Reader>(null);

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
    @NonNull
    public static void getInstance(Context context, final ReaderListener listener) {
        // If uniqueAskReaderInstance is null, instantiates it
        if (uniqueAskReaderInstance.get() == null) {
            Reader.getInstance(context, new InstanceListener<Reader>() {
                @Override
                public void onCreated(Reader reader) {

                    // Opens reader
                    int ret = reader.cscOpen(
                        fr.coppernic.sdk.core.Defines.SerialDefines.ASK_READER_PORT,
                        115200,
                        false);

                    if (ret != Defines.RCSC_Ok) {
                        listener.onError(ret);
                    }

                    // Initializes reader
                    StringBuilder sb = new StringBuilder();
                    ret = reader.cscVersionCsc(sb);

                    // Stores the instance
                    AndroidCone2AskReaderFactory.uniqueAskReaderInstance = new WeakReference<Reader>(reader);

                    if (ret != Defines.RCSC_Ok) {
                        listener.onError(ret);
                    } else {
                        listener.onInstanceAvailable(AndroidCone2AskReaderFactory
                                .uniqueAskReaderInstance
                                .get());
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


    /**
     * Returns the unique instance of ASK reader. This should not be called as long as
     * getInstance(Context context, final ReaderListener listener) has nor successfully executed.
     * @return Unique Reader instance
     */
    @Nullable
    public static Reader getInstance() {
        return uniqueAskReaderInstance.get();
    }

    public static void clearInstance () {
        uniqueAskReaderInstance.get().cscClose();
        uniqueAskReaderInstance = null;
    }
}
