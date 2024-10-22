package com.pocket.ui.text;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;

/**
 * Methods for adding Pocket icons to Strings.
 */
public class IconFont { //

    public static void addArrow(Context context, SpannableStringBuilder stringBuilder, int position) {
        addIcon(context, stringBuilder, position, "\uE800");
    }

    public static void addDiamond(Context context, SpannableStringBuilder stringBuilder, int position) {
        addIcon(context, stringBuilder, position, "\uE801");
    }

    public static void addPocket(Context context, SpannableStringBuilder stringBuilder, int position) {
        addIcon(context, stringBuilder, position, "\uE802");
    }

    private static void addIcon(Context context, SpannableStringBuilder stringBuilder, int position, String iconChar) {
        stringBuilder.insert(position, iconChar);
        CustomTypefaceSpan icon = new CustomTypefaceSpan("", Fonts.get(context, Fonts.Font.ICONS));
        stringBuilder.setSpan(icon, position, position + 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
    }

}
