package org.eclipse.keyple.plugin.android.cone2;

import org.eclipse.keyple.core.seproxy.AbstractPluginFactory;
import org.eclipse.keyple.core.seproxy.ReaderPlugin;

import fr.coppernic.sdk.ask.Reader;

/**
 * This class is a factory for AndroidCone2Plugin
 */
public class AndroidCone2Factory extends AbstractPluginFactory {

    private final Reader reader;

    public AndroidCone2Factory(Reader reader) {
        this.reader = reader;
    }

    @Override
    public String getPluginName() {
        return AndroidCone2PluginImpl.PLUGIN_NAME;
    }

    @Override
    protected ReaderPlugin getPluginInstance() {
        return new AndroidCone2PluginImpl(reader);
    }
}
