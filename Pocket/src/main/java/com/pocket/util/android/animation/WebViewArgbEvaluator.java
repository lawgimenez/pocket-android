/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pocket.util.android.animation;

import android.animation.TypeEvaluator;

/**
 * This evaluator can be used to perform type interpolation between integer
 * values that represent ARGB colors.
 * 
 * This is a copy of {@link android.animation.ArgbEvaluator} but without the conversion between sRGB and linear
 * to match the web view implementation. (So that framework and web transitions can look identical in Reader.)
 */
public class WebViewArgbEvaluator implements TypeEvaluator<Integer> {
    /**
     * This function returns the calculated in-between value for a color
     * given integers that represent the start and end values in the four
     * bytes of the 32-bit int. Each channel is separately linearly interpolated
     * and the resulting calculated values are recombined into the return value.
     *
     * @param fraction The fraction from the starting to the ending values
     * @param startValue A 32-bit int value representing colors in the
     * separate bytes of the parameter
     * @param endValue A 32-bit int value representing colors in the
     * separate bytes of the parameter
     * @return A value that is calculated to be the linearly interpolated
     * result, derived by separating the start and end values into separate
     * color channels and interpolating each one separately, recombining the
     * resulting values in the same way.
     */
    public Integer evaluate(float fraction, Integer startValue, Integer endValue) {
        float startA = ((startValue >> 24) & 0xff) / 255.0f;
        float startR = ((startValue >> 16) & 0xff) / 255.0f;
        float startG = ((startValue >>  8) & 0xff) / 255.0f;
        float startB = ( startValue & 0xff) / 255.0f;
    
        float endA = ((endValue >> 24) & 0xff) / 255.0f;
        float endR = ((endValue >> 16) & 0xff) / 255.0f;
        float endG = ((endValue >>  8) & 0xff) / 255.0f;
        float endB = ( endValue & 0xff) / 255.0f;
        
        // compute the interpolated color
        float a = startA + fraction * (endA - startA);
        float r = startR + fraction * (endR - startR);
        float g = startG + fraction * (endG - startG);
        float b = startB + fraction * (endB - startB);

        // convert back to the [0..255] range
        a = a * 255.0f;
        r = r * 255.0f;
        g = g * 255.0f;
        b = b * 255.0f;

        return Math.round(a) << 24 | Math.round(r) << 16 | Math.round(g) << 8 | Math.round(b);
    }
}