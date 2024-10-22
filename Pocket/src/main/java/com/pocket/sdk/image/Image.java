package com.pocket.sdk.image;

import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.pocket.app.App;
import com.pocket.app.AppThreads;
import com.pocket.app.PocketApp;
import com.pocket.sdk.image.rule.ImageResizeRule;
import com.pocket.sdk.image.rule.ResizeFillAndCrop;
import com.pocket.sdk.image.rule.ResizeFitToHeight;
import com.pocket.sdk.image.rule.ResizeFitToWidth;
import com.pocket.sdk.image.rule.ResizeFitWithin;
import com.pocket.sdk.offline.cache.Asset;
import com.pocket.sdk.offline.cache.AssetUser;
import com.pocket.sdk.offline.cache.DownloadAuthorization;
import com.pocket.util.android.drawable.CacheableBitmapWrapper;
import com.pocket.util.java.Logs;

import java.io.File;
import java.util.concurrent.CountDownLatch;

/**
 * An easy api for downloading, resizing and get from the {@link ImageCache}.
 * <p>
 * Use one of the build(...) methods to get started.
 *
 * <h3>Examples</h3>
 *
 * <pre>
 *
 *  // Download and store for later use:
 *
 *      Image.build(url, user).cache();
 *
 *  // Get an image in a specific size, blocking until it is ready:
 *
 *      Bitmap image = Image.build(url, user)
 *         .fit(150, 150)
 *          .getNow();
 *
 *  // Async:
 *
 *      Image.build(url, user)
 *          .fitHeight(200)
 *          .getAsync(() -> {});
 *
 * </pre>
 */
public class Image {

    public static Builder build(String srcUrl, AssetUser user) {
        PocketApp app = App.getApp();
        return build(srcUrl, user, app.imageCache(), app.threads());
    }
    
    public static Builder build(Asset src, AssetUser user) {
        PocketApp app = App.getApp();
        return build(src, user, app.imageCache(), app.threads());
    }
    
    public static Builder build(String srcUrl, AssetUser user, ImageCache cache, AppThreads threadHelper) {
        return new Builder(srcUrl, user, cache, threadHelper);
    }

    public static Builder build(Asset src, AssetUser user, ImageCache cache, AppThreads threadHelper) {
        return new Builder(src, user, cache, threadHelper);
    }
    
    /**
     * Configuration of your image request.
     * If you want the Bitmap loaded into memory and returned to you, use one of the sizing methods.
     */
    public static class Builder {
    
        private final String mSrcUrl;
        private final Asset mSrcAsset;
        private final AssetUser mUser;
        private final ImageCache mCache;
        private final AppThreads mThreadHelper;
        
        private ImageResizeRule mSize;
        private boolean mIsDownloadEnabled = true;
        private DownloadAuthorization mDownloadAuth = DownloadAuthorization.ONLY_WHEN_SPACE_AVAILABLE;
        private boolean mRefresh;
        private Bundle mExtras;
        private CallbackThread mCallbackThread = CallbackThread.UI;
        private RequestCancel mCanceller;
    
        private Builder(Builder src) {
            mSrcUrl = src.mSrcUrl;
            mSrcAsset = src.mSrcAsset;
            mUser = src.mUser;
            mCache = src.mCache;
            mThreadHelper = src.mThreadHelper;
            mSize = src.mSize;
            mIsDownloadEnabled = src.mIsDownloadEnabled;
            mDownloadAuth = src.mDownloadAuth;
            mRefresh = src.mRefresh;
            mExtras = src.mExtras;
            mCallbackThread = src.mCallbackThread;
            mCanceller = src.mCanceller;
        }
    
        private Builder(String src, AssetUser user, ImageCache cache, AppThreads threadHelper) {
            mSrcUrl = src;
            mSrcAsset = null;
            mUser = user;
            mCache = cache;
            mThreadHelper = threadHelper;
        }
    
        private Builder(Asset src, AssetUser user, ImageCache cache, AppThreads threadHelper) {
            mSrcUrl = null;
            mSrcAsset = src;
            mUser = user;
            mCache = cache;
            mThreadHelper = threadHelper;
        }
    
        /** Lazy load/get of the bundle */
        private Bundle getExtras() {
            if (mExtras == null) {
                mExtras = new Bundle();
            }
            return mExtras;
        }

        /**
         * Enable downloading. If the image is not locally available, this controls
         * whether or not an attempt should be made to download it.
         * <p>
         * Defaults to true.
         *
         * @param value true to allow downloading if needed, false will give up if not found locally.
         * @see #setDownloadAuthorization(com.pocket.sdk.offline.cache.DownloadAuthorization)
         */
        public Builder setDownloadEnabled(boolean value) {
            mIsDownloadEnabled = value;
            return this;
        }

        /**
         * If {@link #setDownloadEnabled(boolean)} is enabled, this controls the {@link com.pocket.sdk.offline.cache.DownloadAuthorization}
         * that will be used. Defaults to {@link com.pocket.sdk.offline.cache.DownloadAuthorization#ONLY_WHEN_SPACE_AVAILABLE}.
         *
         * @param auth Must not be null
         */
        public Builder setDownloadAuthorization(DownloadAuthorization auth) {
            mDownloadAuth = auth;
            return this;
        }
    
        /**
         * If enabled, then even if the image is already on exist, it will
         * attempt to redownload it. If it fails to redownload, the callback will indicate failure.
         * If setting to true, this will also set {@link #setDownloadEnabled(boolean)} to true.
         * Be sure to also set {@link #setDownloadAuthorization(DownloadAuthorization)}.
         * Defaults to false.
         */
        public Builder setRefresh(boolean value) {
            mRefresh = value;
            if (value) setDownloadEnabled(true);
            return this;
        }

        /**
         * Set optional values that you can later obtain in your callback's {@link Request}.
         */
        public Builder putExtra(String key, String value) {
            getExtras().putString(key, value);
            return this;
        }

        /**
         * Set optional values that you can later obtain in your callback's {@link Request}.
         */
        public Builder putExtra(String key, int value) {
            getExtras().putInt(key, value);
            return this;
        }

        /**
         * Only used for async call request methods like {@link #cache(ImageCachedListener)} or {@link SizedBuilder#getAsync(ImageLoadListener)}.
         * Describe what kind of thread you'd like the callback on.
         * Defaults to {@link CallbackThread#UI}
         */
        public Builder callbackThread(CallbackThread value) {
            mCallbackThread = value;
            return this;
        }

        /**
         * Define a method that checks if the image is still needed.
         * Allows the internal processes to cancel work if your image is no longer needed.
         * <p>
         * There are two times this is guaranteed to be invoked.
         * <ol>
         *     <li>After the image is found to be locally available, before it is loaded into memory to be returned.
         *          This is useful for optimization for requests that might ask for an image but possibly
         *          may immediately no longer need it, such as a scrolling ListView. If the image
         *          is no longer needed, this avoids the cost of loading a bitmap into
         *          memory that is just going to be discarded.
         *
         *     <li>Immediately before invoking the callback, regardless of the success of the load.
         *          This is provided as a convenience so you can be confident the image is still
         *          useful during your callback.
         * </ol>
         * @see RequestCancel#isImageStillNeeded(Request)
         */
        public Builder setCanceller(RequestCancel value) {
            mCanceller = value;
            return this;
        }
    
        /**
         * Fills the provided width and height. The image will be resized to match at least one dimension
         * (either width or height) exactly. The other dimension will be centered and have any extra
         * cropped off.
         *
         * This will guarantee the width and height are completely
         * filled with an image and the resulting image will be exactly the requested width and height.
         *
         * This always allows upscaling of images.
         *
         * @param width The requested width in px
         * @param height The requested height in px
         */
        public SizedBuilder fill(float width, float height) {
            mSize = new ResizeFillAndCrop(width, height);
            return new SizedBuilder(this);
        }
    
        /**
         * Size the image to match the provided width,leaving the height to whatever it
         * ends up being while maintaining the same aspect ratio.
         *
         * @param width The requested width in px
         * @param upscaleEnabled Only applies if the source width is smaller than the requested width.
         *                       In that case, if false, no sizing will occur.
         *                       If true, the result image will scale up to match the width.
         */
        public SizedBuilder fitWidth(int width, boolean upscaleEnabled) {
            mSize = new ResizeFitToWidth(upscaleEnabled, width);
            return new SizedBuilder(this);
        }
    
        /**
         * Size the image to match the provided height,leaving the width to whatever it
         * ends up being while maintaining the same aspect ratio.
         *
         * @param height The requested height in px
         * @param upscaleEnabled Only applies if the source height is smaller than the requested height.
         *                       In that case, if false, no sizing will occur.
         *                       If true, the result image will scale up to match the height.
         * @return
         */
        public SizedBuilder fitHeight(int height, boolean upscaleEnabled) {
            mSize = new ResizeFitToHeight(upscaleEnabled, height);
            return new SizedBuilder(this);
        }
    
        /**
         * Resizes an image to fit within the bounds set by the width and height. If the source image is larger than the bounds, then it will
         * shrink the image, maintaining the aspect ratio, so that both width and height are equal or less than the requested size.
         *
         * If the image is already smaller than the requested size no resizing will occur.
         *
         * @param width The requested width in px
         * @param height The requested height in px
         * @return
         */
        public SizedBuilder fit(int width, int height) {
            mSize = new ResizeFitWithin(width, height);
            return new SizedBuilder(this);
        }

        /**
         * {@link #cache(com.pocket.sdk.image.Image.ImageCachedListener)} with no callback.
         */
        public void cache() {
            cache(null);
        }

        /**
         * Performs the image download/resize request asynchronously.
         * Note: If you actually want the bitmap returned to you, you must first specify a size with one of the sizing methods.
         */
        public void cache(final ImageCachedListener callback) {
            performRequest(this, false, (request, wrapper, result) -> {
                if (callback != null) {
                    callback.onImageCacheAttemptComplete(request, result);
                }
            });
        }
    }
    
    /**
     * A sized builder that has methods that let you request that the bitmap is loaded into memory and returned to you.
     * A size is required for this to protect against accidentally loading a massive bitmap into memory.
     * Only the minimum size needed for your UI should be requested.
     */
    public static class SizedBuilder extends Builder {
    
        private SizedBuilder(Builder src) {
            super(src);
        }
    
        /**
         * Blocking call until completed.
         * @return An image wrapper of the image or null if there was an error downloading, resizing or loading it.
         */
        public CacheableBitmapWrapper getNow() {
            try {
                CacheableBitmapWrapper[] r = new CacheableBitmapWrapper[1];
                CountDownLatch latch = new CountDownLatch(1);
                getAsync((request, wrapper, result) -> {
                    r[0] = wrapper;
                    latch.countDown();
                });
                latch.await();
                return r[0];
            } catch (Throwable ignore) {
                Logs.printStackTrace(ignore);
                return null;
            }
        }
    
        /**
         * Same as {@link #getNow()} but returns the bitmap rather than a {@link com.pocket.util.android.drawable.CacheableBitmapWrapper}.
         * It is strongly recommended that you use {@link #getNow()} if possible as it allows the memory of the bitmap
         * to be managed better. <b>If you use this method you must release the bitmap resources yourself when no longer needed</b>
         *
         * @return The image or null if there was an error downloading, resizing or loading it.
         */
        public Bitmap getRawNow() {
            CacheableBitmapWrapper wrapper = getNow();
            return unpackCacheWrapper(wrapper);
        }
    
        /**
         * Same as {@link #cache()} but after downloading, it will load the image
         * and return it to you asynchronously.
         *
         * @param callback
         * @see #callbackThread(CallbackThread)
         */
        public void getAsync(final ImageLoadListener callback) {
            performRequest(this, true, callback);
        }
    
        /**
         * Same as {@link #cache()} but after downloading, it will load the image
         * and return it to you asynchronously.
         * <p>
         * It is strongly recommended that you use {@link #getAsync(com.pocket.sdk.image.Image.ImageLoadListener)}} if possible as it allows the memory of the bitmap
         * to be managed better. <b>If you use this method you must release the bitmap resources yourself when no longer needed</b>
         *
         * @param callback
         * @see #callbackThread(CallbackThread)
         */
        public void getRawAsync(final RawImageLoadListener callback) {
            getAsync((request, wrapper, result) -> callback.onImageLoaded(request, unpackCacheWrapper(wrapper), result));
        }
        
    }
    
    public static class Request {

        /** Can be null if the url was invalid and could not be parsed into an asset. If null, other values may also be null or invalid. */
        public final Asset asset;
        public final AssetUser assetUser;
        public final ImageResizeRule resize;
        public final String assetSizedPath;
        public final boolean tryFetchFromSource;
        public final DownloadAuthorization downloadAuthorization;
        public final boolean refresh;
        public final boolean returnBitmap;
        public final CallbackThread callbackThread;
        public final ImageReadyCallback callback;
        private final RequestCancel canceller;
        private final AppThreads threads;
        
        private Request(Builder request, boolean returnBitmap, ImageReadyCallback callback) {
            resize = request.mSize;

            asset = request.mSrcAsset != null ? request.mSrcAsset : Asset.createImage(
                    request.mSrcUrl,
                    App.getApp().assets().getAssetDirectoryQuietly()
            );

            this.assetUser = request.mUser;
            if (this.resize != null && asset != null) {
                this.assetSizedPath = new StringBuilder()
                        .append(asset.local.getParent())
                        .append(File.separator)
                        .append(asset.filename)
                        .append("_")
                        .append(resize.getWidthFileName())
                        .append("-")
                        .append(resize.getHeightFileName())
                        .append(".jpg") // TODO support transparent png
                        .toString();
            } else {
                this.assetSizedPath = asset != null ? asset.local.getAbsolutePath() : null;
            }
            this.tryFetchFromSource = request.mIsDownloadEnabled;
            this.downloadAuthorization = request.mDownloadAuth;
            this.refresh = request.mRefresh;
            this.callbackThread = request.mCallbackThread;
            this.returnBitmap = returnBitmap;
            this.callback = callback;
            this.canceller = request.mCanceller;
            this.threads = request.mThreadHelper;
        }
    
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Request{");
            sb.append("assetSizedPath='").append(assetSizedPath).append('\'');
            sb.append(", returnBitmap=").append(returnBitmap);
            sb.append(", callback=").append(callback);
            sb.append('}');
            return sb.toString();
        }
    }
    
    private static void performRequest(Builder request, boolean returnBitmap, final ImageLoadListener callback) {
        final Request info = new Request(request, returnBitmap, new ImageReadyCallback() {
            @Override
            public boolean isImageRequestStillValid(Request request) {
                return request.canceller == null || request.canceller.isImageStillNeeded(request);
            }
            @Override
            public void onImageRequestFinished(Request request, Result result, CacheableBitmapWrapper bitmap) {
                invokeCallback(callback, request, result, bitmap);
            }
        });
        if (info.asset == null) {
            invokeCallback(callback, info, Result.FAILED_PERMANENTLY, null);
        } else {
            CacheableBitmapWrapper wrapper = App.getApp().imageCache().getImage(info);
            if (wrapper != null) {
                // It won't async callback, we need to.
                invokeCallback(callback, info, Result.SUCCESS, wrapper);
            }
        }
    }
    
    private static void invokeCallback(final ImageLoadListener callback, final Request info, final Result result, final CacheableBitmapWrapper bitmap) {
        Runnable invoke = () -> {
            if (info.canceller != null && !info.canceller.isImageStillNeeded(info)) {
                // No longer needed, return bitmap to cache and don't invoke callback.
                if (bitmap != null) {
                    bitmap.setBeingUsed(false);
                }
            } else {
                // Ok to return
                callback.onImageLoaded(info, bitmap, result);
            }
        };
        if (info.callbackThread == null || info.callbackThread == CallbackThread.UI) {
            info.threads.runOrPostOnUiThread(invoke);
        } else if (info.callbackThread == CallbackThread.BACKGROUND) {
            info.threads.runOffUiThread(invoke);
        } else {
            invoke.run();
        }
    }
    
    public interface ImageReadyCallback {
        /**
         * Check if the request is still needed. This is also used to determine priority in the task pool, so don't do heavy operations here.
         * This may also be called from a background thread, so don't access views!
         *
         * @param request The request that was made in the past from this instance.
         * @return true If the the request is still wanted.
         */
        public boolean isImageRequestStillValid(Request request);
        
        /**
         * Called when an image is now available on disk at the requested size OR when it has failed to load.
         *
         * This will be called on the UI Thread if request {@link Request#callbackThread} is {@link CallbackThread#UI}.
         *
         * You MUST call recycle on the ImageRequest when you finish. If you do not use the
         * bitmap, you MUST call setBeingUsed(false) on it.
         *  @param request The request that was made in the past from this instance.
         * @param success true if the requested image is available on disk, false if it failed
         * @param bitmap If the request asked for the bitmap to be returned, this will be it. or null if failed.
         */
        void onImageRequestFinished(Request request, Result success, CacheableBitmapWrapper bitmap);
    }
    
    public enum Result {
        SUCCESS,
        /** The image failed to download or resize due to some error. It may be temporary or may be able to be retried. Uncertain.*/
        FAILED,
        /** The image failed to download and will likely never succeed (such as a 404 error). */
        FAILED_PERMANENTLY
    }
    
    public enum CallbackThread {
        /** Calls back on the Android UI/Main thread. */
        UI,
        /** Calls back in a background thread. You should still avoid long operations here as they will block/slow down the image pool. */
        BACKGROUND,
        /** Any thread will do. */
        ANY
    }
    
    private static Bitmap unpackCacheWrapper(CacheableBitmapWrapper wrapper) {
        if (wrapper != null && wrapper.hasValidBitmap()) {
            return wrapper.getBitmap().copy(wrapper.getBitmap().getConfig(), false);
        } else {
            return null;
        }
    }

    public interface RequestCancel {
        /**
         * See {@link com.pocket.sdk.image.Image.Builder#setCanceller(RequestCancel)}
         * for usage details.
         * <p>
         * If you no longer need the image return false. Note, if you return false, you will not receive
         * any further callbacks for this request.
         * <p>
         * Also note that this is used to determine priority in the task pool, so don't do heavy operations here.
         * This will likely be called from a background thread so do not access ui-thread restricted methods.
         */
        boolean isImageStillNeeded(Request info);
    }
    
    /**
     * A helper implementation of {@link RequestCancel} that lets you call {@link #cancel()}
     * to signal that the image is no longer needed.
     */
    public static class Canceller implements RequestCancel {
        private boolean cancelled;
        
        @Override public boolean isImageStillNeeded(Request info) {
            return !cancelled;
        }
        
        public void cancel() {
            cancelled = true;
        }
    }

    public interface ImageLoadListener {

        /**
         * Callback to {@link com.pocket.sdk.image.Image.SizedBuilder#getAsync(com.pocket.sdk.image.Image.ImageLoadListener)}
         * @param request Info about the request that asked for this image.
         * @param wrapper Can be null if there was a problem loading the image.
         * @param result
         */
        void onImageLoaded(Request request, @Nullable CacheableBitmapWrapper wrapper, Result result);

    }

    public interface RawImageLoadListener {

        /**
         * Callback to {@link com.pocket.sdk.image.Image.SizedBuilder#getRawAsync(com.pocket.sdk.image.Image.RawImageLoadListener)}
         *  @param request Info about the request that asked for this image.
         * @param bitmap Can be null if there was a problem loading the image.
         * @param result
         */
        void onImageLoaded(Request request, @Nullable Bitmap bitmap, Result result);

    }

    public interface ImageCachedListener {
        void onImageCacheAttemptComplete(Request request, Result result);
    }
    
}
