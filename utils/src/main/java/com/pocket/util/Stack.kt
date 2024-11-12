package com.pocket.util

/**
 * A stack similar to a java stack, but it won't crash if you try to pop an empty list
 * and it will return null when peeking an empty list
 */
class Stack<T> {

    private val internalList = mutableListOf<T>()

    fun push(value: T) = internalList.add(value)

    fun pop() {
        if (internalList.isEmpty()) {
            println("List is empty.")
        } else {
            internalList.removeAt(internalList.lastIndex)
        }
    }

    fun clear() = internalList.clear()

    fun peek(): T? {
        return if (internalList.isEmpty()) {
            println("List is empty.")
            null
        } else {
            internalList[internalList.lastIndex]
        }
    }
}
