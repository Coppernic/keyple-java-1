package org.eclipse.keyple.plugin.android.cone2;

import java.util.Map;

public class AndroidCone2Parameters {
    /**
     * This parameter sets the timeout used in the waitForCardAbsent method
     */
    public static final String CHECK_FOR_ABSENCE_TIMEOUT_KEY = "CHECK_FOR_ABSENCE_TIMEOUT_KEY";
    /**
     * Default value for CHECK_FOR_ABSENCE_TIMEOUT parameter
     */
    public static final String CHECK_FOR_ABSENCE_TIMEOUT_DEFAULT = "500";
    /**
     *  This parameter sets the thread wait timeout
     */
    public static final String THREAD_WAIT_TIMEOUT_KEY = "THREAD_WAIT_TIMEOUT_KEY";
    /**
     * Default value for THREAD_WAIT_TIMEOUT parameter
     */
    public static final String THREAD_WAIT_TIMEOUT_DEFAULT = "2000";
    /**
     *  This parameter sets the default function timeout in CpcAsk API
     */
    public static final String FUNCTION_TIMEOUT_KEY = "FUNCTION_TIMEOUT_KEY";
    /**
     * Default value for FUNCTION_TIMEOUT parameter
     */
    public static final String FUNCTION_TIMEOUT_DEFAULT = "5000";

    /**
     * Gets a parameter stored as String and converts it to Integer.
     * If parameter has been incorrectly stored as non-Integer, default value will be used.
     * If default value is non-integer, returns 0.
     * @param param Parameter key
     * @param defaultValue Default parameter value
     * @return Parameter value, as an integer
     */
    static int getIntParam(Map<String, String> parameters, String param, String defaultValue) throws NumberFormatException {
        try {
            return Integer.parseInt(parameters.get(param));
        } catch (NumberFormatException nfe) {
            return Integer.parseInt(defaultValue);
        }
    }
}
