package com.pocket.util.java

class MutableClock(var time: Long) : Clock {
    override fun now(): Long {
        return time
    }
}
