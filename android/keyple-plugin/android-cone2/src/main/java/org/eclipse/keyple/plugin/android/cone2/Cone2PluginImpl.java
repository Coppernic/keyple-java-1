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

/**
 * Enables Keyple to communicate with the the C-One² ASK RFID reader.
 */

final class Cone2PluginImpl extends AbstractStaticPlugin implements Cone2Plugin {
    private static final Logger LOG = LoggerFactory.getLogger(Cone2PluginImpl.class);

    private final Map<String, String> parameters = new HashMap<String, String>();// not in use in this
    // plugin

    Cone2PluginImpl() {
        super(PLUGIN_NAME);
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
     * For an Android C-One² device, the Android C-One² Plugin manages only one
     * {@link Cone2ContactlessReaderImpl} and 2 {@link Cone2ContactReaderImpl} .
     * 
     * @return SortedSet<ProxyReader> : contains only one element, the
     *         singleton {@link Cone2ContactlessReaderImpl}
     */
    @Override
    protected SortedSet<SeReader> initNativeReaders() {
        LOG.debug("InitNativeReader() add the unique instance of AndroidCone2Reader");
        SortedSet<SeReader> readers = new TreeSet<SeReader>();
        readers.add(new Cone2ContactlessReaderImpl());
        Cone2ContactReaderImpl sam1 = new Cone2ContactReaderImpl();
        sam1.setParameter(Cone2ContactReader.CONTACT_INTERFACE_ID
                , Cone2ContactReader.CONTACT_INTERFACE_ID_SAM_1);
        readers.add(sam1);
        Cone2ContactReaderImpl sam2 = new Cone2ContactReaderImpl();
        sam2.setParameter(Cone2ContactReader.CONTACT_INTERFACE_ID,
                Cone2ContactReader.CONTACT_INTERFACE_ID_SAM_2);
        readers.add(sam2);
        return readers;
    }

    /**
     * Returns the C-One² Reader whatever is the provided name
     * 
     * @param name : name of the reader to retrieve
     * @return instance of @{@link Cone2ContactlessReaderImpl}
     */
    @Override
    protected SeReader fetchNativeReader(String name) throws KeypleReaderException {
        // return the current reader if it is already listed
        for (SeReader reader : readers) {
            if (reader.getName().equals(name)) {
                return reader;
            }
        }

        throw new KeypleReaderException("Reader " + name + " not found!");
    }
}
