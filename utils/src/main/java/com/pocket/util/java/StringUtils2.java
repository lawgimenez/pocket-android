package com.pocket.util.java;

import org.apache.commons.lang3.StringUtils;

/**
 * Some additional methods that aren't available in {@link org.apache.commons.lang3.StringUtils}.
 */
public class StringUtils2 {

    /**
     * @param str The string to look for (needle)
     * @param choices The strings to search (haystack)
     * @return true if str equals one of the values in choices.
     */
    public static boolean equalsIgnoreCaseOneOf(CharSequence str, CharSequence... choices) {
        for (CharSequence choice : choices) {
            if (StringUtils.equalsIgnoreCase(str, choice)) {
                return true;
            }
        }
        return false;
    }
}
