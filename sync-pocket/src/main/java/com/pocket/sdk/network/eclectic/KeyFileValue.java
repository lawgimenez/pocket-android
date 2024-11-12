package com.pocket.sdk.network.eclectic;

import java.io.File;

public class KeyFileValue {

    public final String key;
    public final File value;

    public KeyFileValue(String key, File value) {
        this.key = key;
        this.value = value;
    }

}