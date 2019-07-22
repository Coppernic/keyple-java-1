package org.eclipse.keyple.plugin.android.cone2;

import android.content.Context;

import org.eclipse.keyple.core.seproxy.plugin.AbstractStaticPlugin;

import java.util.concurrent.locks.ReentrantLock;

import fr.coppernic.sdk.ask.Reader;
import fr.coppernic.sdk.utils.io.InstanceListener;

/**
 * This class is a factory for AndroidCone2Plugin
 */
public class AndroidCone2Factory {

    private static ReentrantLock lock = new ReentrantLock();
    private static AndroidCone2Plugin plugin;

    public interface PluginFactoryListener {
        void onInstantiated(AbstractStaticPlugin plugin);
    }

    /**
     * Returns the unique instance of AndroidCone2Plugin as an AbstractStaticPlugin. Reader must be
     * powered on before calling getPlugin.
     * @param context A context
     * @return AndroidCone2Plugin unique instance
     */
    public static void getPlugin(Context context, final PluginFactoryListener listener) {
        if (plugin == null) {
            Reader.getInstance(context, new InstanceListener<Reader>() {
                @Override
                public void onCreated(Reader reader) {
                    reader.cscOpen("/dev/ttyHSL1", 115200, false);
                    StringBuilder sb = new StringBuilder();
                    reader.cscVersionCsc(sb);
                    plugin = AndroidCone2Plugin.getInstance(reader);
                    listener.onInstantiated(plugin);
                }

                @Override
                public void onDisposed(Reader reader) {

                }
            });
        } else {
            listener.onInstantiated(plugin);
        }
    }
}
