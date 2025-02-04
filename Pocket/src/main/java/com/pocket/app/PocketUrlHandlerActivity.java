package com.pocket.app;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.fragment.app.FragmentActivity;

import com.ideashower.readitlater.R;
import com.pocket.app.add.AddActivity;
import com.pocket.sdk.util.DeepLinks;
import com.pocket.util.android.IntentUtils;
import com.pocket.util.android.Timeout;
import com.pocket.util.java.StringUtils2;
import com.pocket.util.java.UrlResolver;

import java.util.ArrayList;

/**
 * Handles view intents for several types of pocket:// and https://getpocket.com urls that
 * can perform native operations in the app.
 */
public class PocketUrlHandlerActivity extends FragmentActivity {
    private Toast mLoadingToast;
    private Timeout mTimeout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    
        // WARNING: This is an exported activity. Extras could come from outside apps and may not be trust worthy.
        Uri uri = getIntent().getData();
        if (uri == null) {
            finish();
            return;
        }

        if (StringUtils2.equalsIgnoreCaseOneOf(
                uri.getAuthority(),
                "email.getpocket.com",
                "e.getpocket.com",
                "wd.getpocket.com",
                "clicks.getpocket.com"
        )) {
            resolveEmailUrl();

        } else if (handleUrl(uri.toString())) {
            // handleUrl will have finished the activity as needed.
        } else {
            // Unsupported Uri or page, just pass to browser
            startBrowser(uri.toString());

        }
    }

    /**
     * If this url is a Pocket url that we know how to handle natively, this will start the new activity and finish this one and return true.
     * Otherwise it will return false.
     * @param url
     * @return
     */
    private boolean handleUrl(String url) {
        if (url == null) {
            return false;
        }

        if (DeepLinks.Parser.isPocketSaveUrl(url)) {
            startAddActivity(url);
            return true;

        } else {
            Intent intent = DeepLinks.Parser.parseLinkFromExternalApplication(this, url, null);
            if (intent != null) {
                startIntent(intent);
                AppOpen appOpen = App.from(this).appOpen();
                appOpen.setDeepLink(url);
                appOpen.setReferrer(ActivityCompat.getReferrer(this));
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Resolves the VIEW url, each step of the way, looking to see if {@link #handleUrl(String)} can handle it.
     * <p>
     * If it is never handled or has any network errors, it will just open in the browser.
     * <p>
     * Handles finishing this Activity at the end of the process either way.
     */
    private void resolveEmailUrl() {
        // Resolve the url and save if it is a save link. Otherwise open in the browser.
        new UrlResolver(getIntent().getDataString(), new UrlResolver.ResolveStepListener() {
            @Override
            public boolean onPreUrlResolve(ArrayList<String> urls, String next) {
                return !handleUrl(next);
            }

            @Override
            public void onUrlFullyResolved(ArrayList<String> urls) {
                onNoDeepLinkFound(urls);
            }

            @Override
            public void onUrlResolveError(ArrayList<String> urlsSoFar) {
                onNoDeepLinkFound(urlsSoFar);
            }

            private void onNoDeepLinkFound(final ArrayList<String> urls) {
                 /*
                 If there were any redirects, we want to open whatever url was immediately after the email url.
                 We don't open the email url because that would track an email click twice.
                 We don't open later urls because how they redirected might be dependant on
                 cookies our http client doesn't have but the browser might. For example
                 getpocket.com/addshare would redirect to /login if we don't have cookies
                 for a logged in session, but if we open to /login the user ends up in the wrong location.
                 If we open to getpocket.com/addshare they end up in the right location.
                  */
                String url;
                if (urls.size() == 1) {
                    url = urls.get(0);
                } else {
                    url = urls.get(1);
                }
                startBrowser(url);
            }
        }).resolveAsync();

        // If resolving is slow, eventually show a "Loading..." toast.
        mTimeout = new Timeout(new Timeout.TimeoutListener() {
            @Override
            public void onTimeout(Timeout timeout) {
                if (!isFinishing()) {
                    if (mLoadingToast == null) {
                        mLoadingToast = Toast.makeText(PocketUrlHandlerActivity.this, R.string.dg_loading, Toast.LENGTH_LONG);
                    }
                    mLoadingToast.show();
                }
            }
        }).start(1000);
    }

    private void hideLoadingToast() {
        if (mTimeout != null) {
            mTimeout.cancel();
        }
        if (mLoadingToast != null) {
            mLoadingToast.cancel();
        }
    }

    private void startAddActivity(final String url) {
        App.getApp().threads().runOrPostOnUiThread(new StartActivityRunnable() {
            @Override
            public void performStartActivity() {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        .setComponent(new ComponentName(PocketUrlHandlerActivity.this, AddActivity.class)));
            }
        });
    }

    private void startBrowser(final String url) {
        App.getApp().threads().runOrPostOnUiThread(new StartActivityRunnable() {
            @Override
            public void performStartActivity() {
                if (url != null) {
                    IntentUtils.openWithDefaultBrowser(PocketUrlHandlerActivity.this, new Intent(Intent.ACTION_VIEW, Uri.parse(url)), false); // Must pick an explicit browser to avoid infinite loops
                }
            }
        });
    }

    private void startIntent(final Intent intent) {
        App.getApp().threads().runOrPostOnUiThread(new StartActivityRunnable() {
            @Override
            public void performStartActivity() {
                TaskStackBuilder.create(PocketUrlHandlerActivity.this)
                        .addNextIntentWithParentStack(intent)
                        .startActivities();
            }
        });
    }

    /**
     * Wraps your start call to safely handle the url resolver being off the ui thread and also to
     * account for the user canceling the resolving.
     */
    private abstract class StartActivityRunnable implements Runnable {

        @Override
        public final void run() {
            if (!isFinishing()) { // User might have cancelled
                hideLoadingToast();
                performStartActivity();
                finish();
            }
        }

        /**
         * Start the activity. This current one will be finished for you after this call.
         */
        public abstract void performStartActivity();
    }

    public static boolean isInterceptingDisabled(Uri uri) {
        return uri.getBooleanQueryParameter("no_app_intercept", false);
    }

}
