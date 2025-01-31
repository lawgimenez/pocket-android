package com.pocket.sdk.api.value

/**
 * A string that contains markdown markup.
 * 
 * @param value The original raw string.
 */
data class MarkdownString(val value: String) {
    /**
     * Platforms (like Android) use this to supply a platform specific way to parse the raw
     * markdown string into a platform specific parsed type T.
     */
    fun interface Parser<T> {
        fun parse(mdString: MarkdownString): T
    }

    private var parser: Parser<*>? = null
    private var parsed: Any? = null
    
    /**
     * The parsed string.
     *
     *
     * Note: The first time this is invoked, it will perform the parsing and cache it, future
     * calls to this method will get it immediately from the cache as long as you pass the same
     * parser.
     *
     *
     * Warning: For caching/optimization purposes this will hold a reference to the `parser`.
     * It is expected to use a singleton/static implementation.
     */
    fun <T> parsed(parser: Parser<T>): T {
        if (parsed == null || parser != this.parser) {
            parsed = parser.parse(this)
            this.parser = parser
        }

        @Suppress("UNCHECKED_CAST")
        return parsed as T
    }
}
