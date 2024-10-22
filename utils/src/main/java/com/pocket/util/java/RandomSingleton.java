package com.pocket.util.java;

import java.util.Random;

/**
 * A shared, single instance of {@link Random}.
 */
public enum RandomSingleton {

    INSTANCE;

    private Random random = new Random();

    public static Random get() {
        return INSTANCE.random;
    }

}
