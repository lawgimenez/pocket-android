package com.pocket.app.reader.internal.article.javascript

import org.apache.commons.lang3.StringEscapeUtils

class JavascriptFunction(
    private val functionName: String,
) {

    private var parameters = mutableListOf<Any>()

    fun getCommand(): String = "article.$functionName(${
        parameters.joinToString(
            separator = ", "
        )
    });"

    fun addParameter(value: String) {
        var parameter = value
        // Properly escape special characters or transform encoded ' into ' so we can escape them.
        parameter = StringEscapeUtils.escapeJava(parameter)
        parameter = parameter.replace("(?<!\\\\)'".toRegex(), "\\\\'") // Escape ' if not escaped
        parameter = "'$parameter'"
        parameters.add(parameter)
    }

    fun addJsonStringParameter(value: String) {
        var parameter = value
        // Escape some characters that break things if they aren't already escaped.
        val notEscapedPrefix = "(?<!\\\\)"
        parameter = parameter.replace((notEscapedPrefix + "\u2028").toRegex(), "\\\\u2028")
        parameter = parameter.replace((notEscapedPrefix + "\u2029").toRegex(), "\\\\u2029")
        parameters.add(parameter)
    }

    fun addParameter(value: Int) {
        parameters.add(value)
    }

    fun addParameter(value: Float) {
        parameters.add(value)
    }

    fun addParameter(value: Double) {
        parameters.add(value)
    }

    fun addParameter(value: Long) {
        parameters.add(value)
    }

    fun addParameter(value: Boolean) {
        parameters.add(if (value) {
            "true"
        } else {
            "false"
        })
    }
}