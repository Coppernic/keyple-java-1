package org.eclipse.keyple.plugin.android.cone2;

import android.content.Context;

import org.eclipse.keyple.core.seproxy.plugin.AbstractStaticPlugin;

import fr.coppernic.sdk.ask.Reader;

/**
 * This class is a factory for AndroidCone2Plugin
 */
public class AndroidCone2Factory {

    private static AndroidCone2Plugin uniquePluginInstance;

    public interface PluginFactoryListener {
        void onInstanceAvailable(AbstractStaticPlugin plugin);
        void onError(int error);
    }

    /**
     * Returns the unique instance of AndroidCone2Plugin as an AbstractStaticPlugin. Reader must be
     * powered on before calling getPlugin.
     * @param context A context
     */
    public static void getPlugin(Context context, final PluginFactoryListener listener) {
        if (uniquePluginInstance == null) {
            AndroidCone2AskReader.getInstance(context, new AndroidCone2AskReader.ReaderListener() {
                @Override
                public void onInstanceAvailable(Reader reader) {
                    uniquePluginInstance = AndroidCone2Plugin.getInstance();
                    listener.onInstanceAvailable(uniquePluginInstance);
                }

                @Override
                public void onError(int error) {
                    listener.onError(error);
                }
            });
        } else {
            listener.onInstanceAvailable(uniquePluginInstance);
        }
    }
}
