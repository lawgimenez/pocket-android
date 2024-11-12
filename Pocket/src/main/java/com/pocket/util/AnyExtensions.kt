package com.pocket.util

fun <T> T.equalsAny(vararg values: T): Boolean {
    values.forEach { value ->
        if (this == value) {
            return true
        }
    }
    return false
}