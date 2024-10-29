package com.pocket.sdk.api.value;

import org.apache.commons.lang3.StringUtils;

/**
 *  A String that contains html as a string. Could be fairly large depending on the size of the webpage.
 */
public class HtmlBlob {

    public final String value;

    public HtmlBlob(String value) {
        this.value = value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HtmlBlob that = (HtmlBlob) o;
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
    
    public static boolean isBlank(HtmlBlob value) {
        return value == null || StringUtils.isBlank(value.value);
    }
    
    public static String toString(HtmlBlob value) {
        return value != null ? value.toString() : null;
    }
}
