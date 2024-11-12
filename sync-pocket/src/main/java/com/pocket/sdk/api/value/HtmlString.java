package com.pocket.sdk.api.value;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

/**
 *  A String that may contain a limited set of html tags for formatting like for bold, italic, etc. TODO what subset is supported?
 */
public class HtmlString {
    
    /**
     * Platforms (like Android) use this to supply a platform specific way to parse the raw
     * html string into a platform specific parsed type T.
     */
    public interface Parser<T> {
        T parse(HtmlString htmlString);
    }

    /** The original raw string. */
    public final String value;
    
    private Parser<?> parser;
    private Object parsed;

    public HtmlString(String value) {
        this.value = value;
    }
    
    /**
     * The parsed string.
     * <p>
     * Note: The first time this is invoked, it will perform the parsing and cache it, future 
     * calls to this method will get it immediately from the cache as long as you pass the same 
     * parser.
     * <p>
     * Warning: For caching/optimization purposes this will hold a reference to the {@code parser}.
     * It is expected to use a singleton/static implementation.
     */
    public <T> T parsed(Parser<T> parser) {
        if (parsed == null || !parser.equals(this.parser)) {
            parsed = parser.parse(this);
            this.parser = parser;
        }
        //noinspection unchecked
        return (T) parsed;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HtmlString that = (HtmlString) o;
        return value != null ? value.equals(that.value) : that.value == null;
    }
    
    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }
    
    @Nullable
    public static <T> T parsed(@Nullable HtmlString value, Parser<T> parser) {
        return value != null ? value.parsed(parser) : null;
    }
    
    @Override
    public String toString() {
        return value;
    }
    
    public static boolean isBlank(HtmlString value) {
        return value == null || StringUtils.isBlank(value.value);
    }
    
    public static String toString(HtmlString value, Parser<?> parser) {
        return value != null ? value.parsed(parser).toString() : null;
    }
}
