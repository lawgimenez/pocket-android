package com.pocket.ui.util;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.pocket.ui.R;
import com.pocket.ui.text.Fonts;

import java.util.Arrays;

/**
 * Article placeholder images, based on:
 *
 * https://www.figma.com/file/fWihuFRYxl21bUny8zr7we/Listen?node-id=1880%3A182
 */
public class PlaceHolderBuilder {

    public enum PktColor {

        CORAL(R.color.pkt_coral_5, R.color.pkt_themed_coral_2),
        AMBER(R.color.pkt_amber_faint, R.color.pkt_themed_amber_1),
        TEAL(R.color.pkt_teal_6, R.color.pkt_themed_teal_3),
        BLUE(R.color.pkt_lapis_faint, R.color.pkt_themed_lapis_3);

        @ColorRes
        final int background;

        @ColorRes
        final int text;

        PktColor(@ColorRes int background, @ColorRes int text) {
            this.background = background;
            this.text = text;
        }

    }

    public enum Corner {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    private PlaceHolderBuilder() {
        //
    }

    /**
     * Gets a new {@link PlaceHolderDrawable} of the provided character, color, and corner.
     *
     * @param context
     * @param character The char to display in the image.
     * @param color A {@link PktColor} to use for the background and text colors.
     * @param corner A {@link Corner} at which to align the character.
     * @return a {@link PlaceHolderDrawable}
     */
    public static Drawable getDrawable(Context context, char character, PktColor color, Corner corner) {
        return new PlaceHolderDrawable(context, character, color.background, color.text, corner);
    }

    /**
     * Gets a new {@link PlaceHolderDrawable}.
     *
     * @param context
     * @param id A unique id to use when creating the drawable.  The String's hashcode is used to determine
     *           which color and corner will be displayed, ensuring that any String will have the same color / corner
     *           configuration.
     * @param character the char to display in the drawable.
     * @return a {@link PlaceHolderDrawable}
     */
    public static Drawable getDrawable(Context context, String id, char character) {
        return getDrawable(context, id.hashCode(), character);
    }

    /**
     * Gets a new {@link PlaceHolderDrawable}.
     *
     * @param context
     * @param id A unique id to use when creating the drawable.  This ensures that any id will
     *           have the same color / corner configuration.
     * @param character the char to display in the drawable.
     * @return a {@link PlaceHolderDrawable}
     */
    public static Drawable getDrawable(Context context, int id, char character) {

        // simple mod of the id by the number of colors
        PktColor color = Arrays.asList(PktColor.values()).get(Math.abs(id) % PktColor.values().length);

        // same for corners
        Corner corner = Arrays.asList(Corner.values()).get(Math.abs(id) % Corner.values().length);

        return new PlaceHolderDrawable(context, character, color.background, color.text, corner);
    }

    private static class PlaceHolderDrawable extends Drawable {

        /** The percentage greater that the outside bounds is than the actual size of the image */
        private static final float OUTER_REC_MULTIPLIER = 1.55555555556f;

        /** The percentage to increase the size of the font, as compared to the length of a side */
        private static final float FONT_SIZE_MULTIPLIER = 2.00892857143f;

        private final Paint backgroundPaint = new Paint();
        private final TextPaint textPaint = new TextPaint();
        private final Rect textbounds = new Rect();

        private final ColorStateList backgroundColor;
        private final ColorStateList textColor;
        private final Corner corner;
        private final String character;

        private int visibleSideLength;
        private int outsideMargin;
        private int halfFullLength;

        private PlaceHolderDrawable(Context context, char character, @ColorRes int backgroundColor, @ColorRes int textColor, Corner corner) {
            this.character = Character.toString(character);
            this.backgroundColor = ContextCompat.getColorStateList(context, backgroundColor);
            this.textColor = ContextCompat.getColorStateList(context, textColor);
            this.corner = corner;

            backgroundPaint.setAntiAlias(true);

            textPaint.setAntiAlias(true);
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setTypeface(Fonts.get(context, Fonts.Font.DOYLE_MEDIUM));

            updatePaint(getState());
        }

        @Override
        public boolean isStateful() {
            return true;
        }

        @Override
        protected boolean onStateChange(int[] state) {
            updatePaint(state);
            return true;
        }

        private void updatePaint(int[] state) {
            backgroundPaint.setColor(backgroundColor.getColorForState(state, Color.TRANSPARENT));
            textPaint.setColor(textColor.getColorForState(state, Color.TRANSPARENT));
        }

        @Override
        protected void onBoundsChange (Rect bounds) {
            visibleSideLength = bounds.height();
            outsideMargin = ((int) (visibleSideLength * OUTER_REC_MULTIPLIER) - visibleSideLength) / 2;
            halfFullLength = (visibleSideLength + 2 * outsideMargin) / 2;
            textPaint.setTextSize((int) (FONT_SIZE_MULTIPLIER * visibleSideLength));
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect bgbounds = getBounds();
            canvas.clipRect(bgbounds.left, bgbounds.top, bgbounds.right, bgbounds.bottom, Region.Op.INTERSECT);
            canvas.drawRect(bgbounds.left, bgbounds.top, bgbounds.right, bgbounds.bottom, backgroundPaint);

            textPaint.getTextBounds(character, 0, 1, textbounds);

            canvas.translate(getXTranslate(textbounds.width()), getYTranslate(textbounds.height()));
            canvas.drawText(character, -textbounds.left, -textbounds.top, textPaint);
        }

        private int getXTranslate(int textWidth) {
            int translate;
            if (corner == Corner.BOTTOM_LEFT || corner == Corner.TOP_LEFT) {
                // if the width of the text is less than half the full width (including outside margin), align the right side of the text to the center of the view
                if (textWidth < halfFullLength) {
                    translate = (visibleSideLength / 2) - textWidth;
                } else {
                    translate = -outsideMargin;
                }
            } else { // BOTTOM_RIGHT || TOP_RIGHT
                // if the width of the text is less than half the full width (including outside margin), align the left side of the text to the center of the view
                if (textWidth < halfFullLength) {
                    translate = visibleSideLength / 2;
                } else {
                    translate = (visibleSideLength - textWidth) + outsideMargin;
                }
            }
            return translate;
        }

        private int getYTranslate(int textHeight) {
            int translate;
            // if the height of the text is less than half the full height (including outside margin), just center vertically
            if (textHeight < halfFullLength) {
                translate = (visibleSideLength - textHeight) / 2;
            } else {
                // otherwise, follow vertical shifting rules
                if (corner == Corner.BOTTOM_LEFT || corner == Corner.BOTTOM_RIGHT) {
                    translate = (visibleSideLength - textHeight) + outsideMargin;
                } else { // TOP_LEFT || TOP_RIGHT
                    translate = -outsideMargin;
                }
            }
            return translate;
        }

        @Override
        public void setAlpha(int alpha) {
            backgroundPaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            backgroundPaint.setColorFilter(cf);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

}
