package com.pocket.util.android.drawable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;

/**
 * Some canvas drawing operations and features such as xfermodes are not available on hardware
 * canvases. This class simplifies drawing to an offscreen software canvas and then rendering
 * that onto your original canvas.
 * <p>
 * Anytime the bounds of your canvas changes, such as {@link View#onSizeChanged(int,int,int,int)}
 * or {@link Drawable#onBoundsChanged(Rect)}, be sure to invoke this classes {@link #onBoundsChanged(Rect)}
 * method.
 * <p>
 * There are two ways to draw with this.
 * <p>
 * <b>1. Using a {@link com.pocket.util.android.drawable.SoftwareCanvas.Drawer}.</b>
 * In your onDraw() method just call {@link #draw(Canvas)} and then use {@link Drawer#drawSoftware(Canvas)}
 * to do your actual drawing.
 *
 * <b>2. Manually</b>
 * Grab a cleared canvas with {@link #getCanvas()}, do your drawing operations and then
 * use {@link #getBitmap()} to get the result, or use {@link #applyCanvas(Canvas)} to
 * draw the result onto another canvas.
 */
@Deprecated // This is pretty bad for performance and memory so use software layers instead if possible.
public class SoftwareCanvas {

    private final Drawer mDrawer;

    private Bitmap mBitmap;
    private Canvas mCanvas;

    public SoftwareCanvas(Drawer drawer) {
        mDrawer = drawer;
    }

    public void onBoundsChanged(Rect bounds) {
        if (mBitmap == null
         || mBitmap.getWidth() != bounds.width()
         || mBitmap.getHeight() != bounds.height()) {
            makeNewBitmap(bounds.right, bounds.bottom);
        }
    }

    public void draw(Canvas canvas) {
        if (mBitmap == null) {
            return; // No bounds set
        }
        mDrawer.drawSoftware(getCanvas());
        canvas.drawBitmap(mBitmap, 0, 0, null);
    }

    public Canvas getCanvas() {
        mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        return mCanvas;
    }

    public Bitmap getBitmap() {
        return mBitmap;
    }

    private void makeNewBitmap(int width, int height) {
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
        if (width > 0 && height > 0) {
            mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
        }
    }

    public interface Drawer {
        public void drawSoftware(Canvas canvas);
    }

}
