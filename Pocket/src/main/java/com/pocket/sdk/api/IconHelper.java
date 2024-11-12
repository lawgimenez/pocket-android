package com.pocket.sdk.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;

import com.pocket.app.App;
import com.pocket.util.java.Logs;
import com.pocket.sdk.image.Image;
import com.pocket.sdk.offline.cache.AssetUser;
import com.pocket.sdk.offline.cache.DownloadAuthorization;
import com.pocket.sdk.api.generated.thing.Icon;

/**
 * Helper for working with {@link com.pocket.sdk.api.generated.thing.Icon}s.
 */
public class IconHelper {

    private final Icon data;
    private BitmapDrawable drawable;
    private int density;

    public IconHelper(Icon data) {
        this.data = data;
    }

    /**
     * Returns a verison of this image, for the current screen density.
     * <p/>
     * If cached, it will invoke the callback immediately on this invoking thread, otherwise it will fetch it asynchronously
     * and notify (on the ui thread) the optional callback when it is available.
     *
     * @param context
     * @param callback
     * @return
     */
    public void getBitmap(final Context context, final OnImageReadyListener callback) {
        final DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        final int targetDensity = metrics.densityDpi;
        if (drawable != null && density == targetDensity) {
            // Can return cached version
            if (callback != null) {
                callback.onDensityDependantImageReady(drawable);
            }

        } else {
            // Either not cached or the density has changed and needs to load a new image
            drawable = null;
            density = targetDensity;
    
            String src;
            int inDensity;
            if (density <= 1) {
                src = data._1x;
                inDensity = DisplayMetrics.DENSITY_MEDIUM;
            } else if (density <= 1.34 && data._1_33x != null) {
                src = data._1_33x;
                inDensity = 213; // Compat for DisplayMetrics.DENSITY_TV;
            } else if (density <= 1.5) {
                src = data._1_5x;
                inDensity = DisplayMetrics.DENSITY_HIGH;
            } else if (density <= 2) {
                src = data._2x;
                inDensity = 320; // Compat for DisplayMetrics.DENSITY_XHIGH;
            } else if (density <= 3) {
                src = data._3x;
                inDensity = 480; // Compat for DisplayMetrics.DENSITY_XXHIGH;
            } else {
                src = data._4x;
                inDensity = 640; // Compat for DisplayMetrics.DENSITY_XXHIGH;
            }

            Image.build(src, AssetUser.forApp())
                    .setDownloadAuthorization(DownloadAuthorization.ALWAYS)
                    .cache((request, isCached) -> {
                        // The image request is complete, it is either on disk or failed.
                        if (request.asset != null) loadBitmap(context, request.asset.local.getAbsolutePath(), inDensity, targetDensity, callback);
                    });
        }
    }

    private void loadBitmap(final Context context, String path, int inDensity, final int targetDensity, final OnImageReadyListener callback) {
        // Note: This is likely being invoked off the ui thread.
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScreenDensity = targetDensity;
        opts.inTargetDensity = targetDensity;
        opts.inDensity = inDensity;
        opts.inScaled = true;
        final Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeFile(path, opts);
        } catch (Throwable ignore) {
            Logs.printStackTrace(ignore);
            // Ok to ignore if we were unable to load this image.
            return;
        }
        
        // Ensure on UI Thread
        App.getApp().threads().runOrPostOnUiThread(() -> {
            if (targetDensity != density) {
                return; // Something changed since this request was made. Cancel.
            }
            drawable = new BitmapDrawable(context.getResources(), bitmap);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
    
            if (callback != null) {
                callback.onDensityDependantImageReady(drawable);
            }
        });
    }


    public interface OnImageReadyListener {
        /**
         * Invoked when a icon is downloaded locally and cached in memory.
         * <p/>
         * Always invoked on the UI Thread.
         *
         * @param icon      The icon, never null.
         */
        public void onDensityDependantImageReady(BitmapDrawable icon);
    }


}
