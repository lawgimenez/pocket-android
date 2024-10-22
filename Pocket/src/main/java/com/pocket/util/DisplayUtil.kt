package com.pocket.util

import com.pocket.data.models.Author
import com.pocket.util.java.DomainUtils
import org.apache.commons.lang3.StringUtils

/**
 * Utils used for getting display strings
 */
object DisplayUtil {

    @JvmStatic
    fun displayHost(url: String): String? {
        val host = DomainUtils.getHost(url)
        return StringUtils.replaceOnce(host, "www.", "")
    }

    @JvmStatic
    fun displayAuthors(authors: List<Author>): String? {
        if (authors.isEmpty()) return null
        if (authors.size == 1) return authors[0].name
        val authors: Iterator<Author> = authors.iterator()
        val stringBuilder = StringBuilder(authors.next().name)
        while (authors.hasNext()) {
            stringBuilder.append(", ")
            stringBuilder.append(authors.next().name)
        }
        return stringBuilder.toString()
    }
}