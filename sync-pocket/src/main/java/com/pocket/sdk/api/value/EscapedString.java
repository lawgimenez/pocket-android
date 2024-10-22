package com.pocket.sdk.api.value;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * The original escaped value is available as {@link #value}.
 * To get the unescaped value, use {@link #unescaped()}
 */
public class EscapedString {

    /** The original, escaped value. */
    public final String value;
    private String unescaped;

    public EscapedString(String value) {
        this.value = value;
    }
    
	/**
     * The unescaped string.
     * Note: The first time this is invoked, it will perform the unescape and cache it, future calls to this method will get it immediately from the cache.
     */
    public String unescaped() {
        if (unescaped == null) {
            unescaped = StringEscapeUtils.unescapeHtml3(value);
        }
        return unescaped;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EscapedString that = (EscapedString) o;
        return value != null ? value.equals(that.value) : that.value == null;
    }
    
    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return value;
    }
    
    public static boolean isBlank(EscapedString value) {
        return value == null || StringUtils.isBlank(value.value);
    }
    
    public static String unescaped(EscapedString value) {
        return value != null ? value.unescaped() : null;
    }
}
