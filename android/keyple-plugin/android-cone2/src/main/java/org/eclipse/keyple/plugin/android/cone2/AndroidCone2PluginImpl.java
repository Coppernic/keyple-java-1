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
package org.eclipse.keyple.plugin.android.cone2;

import org.eclipse.keyple.core.seproxy.SeReader;
import org.eclipse.keyple.core.seproxy.exception.KeypleReaderException;
import org.eclipse.keyple.core.seproxy.plugin.AbstractStaticPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import fr.coppernic.sdk.ask.Reader;


/**
 * Enables Keyple to communicate with the the Android device embedded NFC reader. In the Android
 * platform, NFC reader must be link to an application activity.
 *
 *
 *
 *
 *
 */

final class AndroidCone2PluginImpl extends AbstractStaticPlugin implements AndroidCone2Plugin {

    private static final Logger LOG = LoggerFactory.getLogger(AndroidCone2PluginImpl.class);

    static final String PLUGIN_NAME = "AndroidCone2Plugin";

    private final Map<String, String> parameters = new HashMap<String, String>();// not in use in this

    private final Reader reader;
    // plugin

    AndroidCone2PluginImpl(Reader reader) {
        super(PLUGIN_NAME);
        this.reader = reader;
    }

    @Override
    public Map<String, String> getParameters() {
        LOG.warn("Android C-One² Plugin does not support parameters, see AndroidCone2Reader instead");
        return parameters;
    }

    @Override
    public void setParameter(String key, String value) {
        LOG.warn("Android C-One² Plugin does not support parameters, see AndroidCone2Reader instead");
        parameters.put(key, value);
    }


    /**
     * For an Android C-One² device, the Android C-One² Plugin manages only one @{@link AndroidCone2ReaderImpl}.
     * 
     * @return SortedSet<ProxyReader> : contains only one element, the
     *         singleton @{@link AndroidCone2ReaderImpl}
     */
    @Override
    protected SortedSet<SeReader> initNativeReaders() throws KeypleReaderException {
        LOG.debug("InitNativeReader() add the unique instance of AndroidCone2Reader");
        SortedSet<SeReader> readers = new TreeSet<SeReader>();
        readers.add(new AndroidCone2ReaderImpl(reader));
        return readers;
    }

    /**
     * Return the C-One²Reader whatever is the provided name
     * 
     * @param name : name of the reader to retrieve
     * @return instance of @{@link AndroidCone2ReaderImpl}
     */
    @Override
    protected SeReader fetchNativeReader(String name) throws KeypleReaderException {
        return readers.first();
    }
}
