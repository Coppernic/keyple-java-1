package org.eclipse.keyple.plugin.android.cone2;

import org.eclipse.keyple.core.seproxy.SeReader;

public interface Cone2ContactReader extends SeReader {
    /**
     * THis parameter allows SAM reader selection
     */
    String CONTACT_INTERFACE_ID = "CONTACT_INTERFACE_ID";
    /**
     * Default value for CONTACT_INTERFACE_ID parameter
     */
    String CONTACT_INTERFACE_ID_DEFAULT = "1";
    /**
     * SAM 1
     */
    String CONTACT_INTERFACE_ID_SAM_1 = "1";
    /**
     * SAM 2
     */
    String CONTACT_INTERFACE_ID_SAM_2 = "2";
}
