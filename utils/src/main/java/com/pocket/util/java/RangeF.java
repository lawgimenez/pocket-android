package com.pocket.util.java;

public class RangeF {

    public enum Constrain {
        NONE,
        MIN,
        MAX,
        BOTH
    }

    public final float min;
    public final float max;

    public RangeF(float min, float max) {
        this.min = min;
        this.max = max;
    }

    public float percentOf(float value) {
        return percentOf(min, max, value);
    }

    public float valueOf(float percent, Constrain constrain) {
        return valueOf(min, max, percent, constrain);
    }

    public static float percentOf(float min, float max, float value) {
        if (value <= min) {
            return 0;
        } else if (value >= max) {
            return 1;
        } else {
            return (value - min) / (max - min);
        }
    }

    public static float percentOf(float min, float max, float value, Constrain constrain) {
        if (value < min && (constrain == Constrain.MIN || constrain == Constrain.BOTH)) {
            return 0;
        } else if (value > max && (constrain == Constrain.MAX || constrain == Constrain.BOTH)) {
            return 1;
        } else {
            return (value - min) / (max - min);
        }
    }

    public static float valueOf(float min, float max, float percent, Constrain constrain) {
        if (percent < 0 && (constrain == Constrain.MIN || constrain == Constrain.BOTH)) {
            return min;
        } else if (percent > 1 && (constrain == Constrain.MAX || constrain == Constrain.BOTH)) {
            return max;
        } else {
            return (percent * (max - min)) + min;
        }
    }

    public static float constrain(float min, float max, float value) {
        return Math.max(Math.min(value, max), min);
    }
    
}