package com.pocket.util.android.text;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;

public class BulletTextUtil {

    /**
     * Returns a CharSequence containing a bulleted and properly indented list.
     *
     * @param leadingMargin In pixels, the space between the left edge of the bullet and the left edge of the text.
     * @param lines An array of CharSequences. Each CharSequences will be a separate line/bullet-point.
     * @return
     */
    public static CharSequence makeBulletList(int leadingMargin, CharSequence... lines) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        for (int i = 0; i < lines.length; i++) {
            CharSequence line = lines[i] + (i < lines.length-1 ? "\n" : "");
            Spannable spannable = new SpannableStringBuilder(line);
            spannable.setSpan(new BulletSpan(leadingMargin), 0, spannable.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            sb.append(spannable);
        }
        return sb;
    }

}