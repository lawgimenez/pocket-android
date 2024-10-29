package com.pocket.sdk.http.sentry

/**
 * I hate regex, so I created this class so I could encapsulate building it,
 * expose a nice API and test it.
 */
class ExcludedTargets {
    private val excludes = mutableListOf<Exclude>()

    fun exclude(target: String, mode: Mode) = excludes.add(Exclude(target, mode))

    fun toRegex(): String {
        return excludes.joinToString(
            prefix = "^(?!(",
            separator = "|",
            postfix = ")).*",
        ) {
            val escaped = it.target.replace(".", "\\.")
            when (it.mode) {
                Mode.Exact -> "$escaped$"
                Mode.Prefix -> escaped
            }
        }
    }

    private data class Exclude(
        val target: String,
        val mode: Mode,
    )

    sealed interface Mode {
        data object Exact : Mode
        data object Prefix : Mode
    }
}
