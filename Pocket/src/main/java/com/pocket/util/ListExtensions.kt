package com.pocket.util

fun <T> List<T>.containsAny(vararg values: T): Boolean {
    values.forEach {
        if (contains(it)) {
            return true
        }
    }
    return false
}