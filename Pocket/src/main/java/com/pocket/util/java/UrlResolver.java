package com.pocket.util.java;

import com.ideashower.readitlater.BuildConfig;
import com.pocket.app.App;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Helper class for unwrapping a url to its resolved final url and viewing the steps along the way.
 */
public class UrlResolver {

    private static final boolean DEBUG = BuildConfig.DEBUG && false;

    private final String mOriginalUrl;
    private final ResolveStepListener mStepListener;

    /**
     * @param url The url to resolve
     * @param stepListener Required, may not be null.
     */
    public UrlResolver(String url, ResolveStepListener stepListener) {
        mOriginalUrl = url;
        mStepListener = stepListener;
    }

    /**
     * Resolve asynchronously.
     */
    public void resolveAsync() {
        App.getApp().threads().async(this::resolve);
    }

    /**
     * Resolve now on this thread. Blocking until complete. Note: This performs network operations.
     */
    public void resolve() {
        ArrayList<String> urls = new ArrayList<>();
        urls.add(mOriginalUrl);

        String next = mOriginalUrl;
        if (!mStepListener.onPreUrlResolve(urls, next)) {
            return;
        }
        while (next != null) {
            try {
                next = getNextUrl(next);
                if (next != null) {
                    urls.add(next);
                    boolean continueResolving = mStepListener.onPreUrlResolve(urls, next);
                    if (!continueResolving) {
                        return;
                    }
                }
            } catch (Throwable t) {
                if (DEBUG) log("failed resolving " + next + " " + t);
                mStepListener.onUrlResolveError(urls);
                return;
            }
        }

        mStepListener.onUrlFullyResolved(urls);
    }

    /**
     * Attempts to resolve the provided url.
     * @param urlString The url to resolve.
     * @return The url it redirected to (one step) or null if it did not redirect.
     * @throws Exception
     */
    private static String getNextUrl(String urlString) throws Exception {
        URL url = new URL(urlString);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(false);
        if (connection.getResponseCode() >= 300 && connection.getResponseCode() < 400) {
            // Redirected
            String location = connection.getHeaderField("Location");
            if (location == null || urlString.equals(location)) {
                if (DEBUG) log("getNextUrl: no next url found");
                return null;

            } else {
                if (location.startsWith("/")) {
                    // Relative url, needs to be converted to absolute
                    URL absolute = new URL(new URL(urlString), location);
                    location = absolute.toString();
                }

                if (DEBUG) log("getNextUrl: resolved to " + location);
                return location;
            }

        } else {
            if (DEBUG) log("getNextUrl: not redirected");
            return null;
        }
    }

    private static void log(String log) {
        if (DEBUG) Logs.v("UrlResolver", log);
    }

    /**
     * Callbacks during the resolve process.
     */
    public interface ResolveStepListener {
        /**
         * Invoked before the next url is resolved. Also invoked for the first/original url.
         * <p>
         * <b>Warning:</b> This is invoked from the resolving thread and may not be the ui thread.
         *
         * @param urls The list of urls so far. Index 0 is always the original url. The last index (size-1) is the url it is about to attempt to resolve.
         * @param next The url about to be resolved / checked if it redirects.
         * @return true if it should continue attempting to resolve, false if the process should stop. If it is stopped here, no other callbacks from this interface will be invoked.
         */
        public boolean onPreUrlResolve(ArrayList<String> urls, String next);

        /**
         * Resolving is complete.
         * <p>
         * <b>Warning:</b> This is invoked from the resolving thread and may not be the ui thread.
         *
         * @param urls The list of urls along the redirect path. Index 0 is always the original url. The last index is the final, resolved url.
         */
        public void onUrlFullyResolved(ArrayList<String> urls);

        /**
         * An error occurred.
         * <p>
         * <b>Warning:</b> This is invoked from the resolving thread and may not be the ui thread.
         *
         * @param urlsSoFar The list of urls along the redirect path known so far. Index 0 is always the original url. The last index is latest resolved url before the error.
         */
        public void onUrlResolveError(ArrayList<String> urlsSoFar);
    }

}
