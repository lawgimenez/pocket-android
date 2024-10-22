package com.pocket.sdk.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.Nullable;

import com.pocket.app.App;
import com.pocket.app.MainActivity;
import com.pocket.app.PocketUrlHandlerActivity;
import com.pocket.app.settings.premium.PremiumSettingsActivity;
import com.pocket.sdk.api.generated.thing.ActionContext;
import com.pocket.sdk.tts.ListenDeepLinkActivity;
import com.pocket.sync.value.Parceller;
import com.pocket.util.android.ApiLevel;
import com.pocket.util.java.Logs;
import com.pocket.util.java.StringUtils2;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper methods for opening specific parts of the app.
 * <p>
 * When adding a deep link, be sure to make sure the intent filter for {@link PocketUrlHandlerActivity}
 * handles it properly.
 */
public class DeepLinks {

    private static Intent newSavesIntent(Context context, ActionContext cxt) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_DESTINATION, MainActivity.DeepLinkDestination.SAVES.ordinal());
        return intent;
    }

	/** An intent to the main, Pocket screen. */
	public static Intent newPocketIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_DESTINATION, MainActivity.DeepLinkDestination.DEFAULT.ordinal());
        return intent;
	}

    public static Intent newHomeIntent(Context context, ActionContext cxt) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_DESTINATION, MainActivity.DeepLinkDestination.HOME.ordinal());
        return intent;
    }
    
    public static Intent newSettingsIntent(Context context, ActionContext cxt) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_DESTINATION, MainActivity.DeepLinkDestination.SETTINGS.ordinal());
        return intent;
    }

    public static Intent newTopicDetailsIntent(Context context, String topicId) {
        var intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_DESTINATION, MainActivity.DeepLinkDestination.TOPIC_DETAILS.ordinal());
        intent.putExtra(MainActivity.EXTRA_TOPIC_ID, topicId);
        return intent;
    }

    public static Intent newOpenAppIntent(Context context, ActionContext cxt) {
        Intent intent = new Intent(context, App.from(context).user().getDefaultActivity());
        Parceller.put(intent, AbsPocketActivity.EXTRA_UI_CONTEXT, cxt);
        return intent;
    }

    /**
     * Because of potential security risks in opening external URLs directly in the reader, we should only be doing
     * so with trusted URL sources, such as those from deep links filtered through at intent-filter, which restricts
     * to only those coming from getpocket.com.
     * <p>
     * If this method is used, consider the implications and if it is possible for arbitrary URLs to be opened in this
     * way, in which case you should probably not be doing so.
     */
    public static Intent newReaderIntentFromTrustedUrl(String url, Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_DESTINATION, MainActivity.DeepLinkDestination.READER.ordinal());
        intent.putExtra(MainActivity.EXTRA_URL, url);
        return intent;
    }
    
    public static Intent newListenIntent(String url, Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_DESTINATION, MainActivity.DeepLinkDestination.READER.ordinal());
        var amendedUrl = url;
        if (!url.contains("http")) {
            amendedUrl = "https://" + url;
        }
        intent.putExtra(MainActivity.EXTRA_URL, amendedUrl);
        intent.putExtra(MainActivity.EXTRA_OPEN_LISTEN, true);
        return intent;
    }

    public static Intent newListenIntent(Context context, ActionContext cxt) {
		return ListenDeepLinkActivity.newStartIntent(context, cxt);
	}

    public static Intent newPremiumIntent(Context context) {
        return App.from(context).premium().newPremiumDeeplink(context);
    }

    public static Intent newPremiumSettingsIntent(Context context) {
        return PremiumSettingsActivity.newStartIntent(context);
    }

    public static Intent newDeviceNotificationSettingsIntent(Context context) {
        Intent intent;
        if (ApiLevel.isOreoOrGreater()) {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());

        } else {
            // Unofficial old intent that seems to work fine on emulator with SDK 23.
            intent = new Intent("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("app_package", context.getPackageName());
            intent.putExtra("app_uid", context.getApplicationInfo().uid);
        }
        return intent;
    }

    /**
     * Converts a string url to an intent to open a deep link into the app.
     */
    public static class Parser {

        public enum Source {
            External,
            Internal,
            Reader,
        } 

        private static final Set<Link> SUPPORTED_LINKS = getSupportedLinks();
        private static final Set<Link> SUPPORTED_MOBILE_LINKS = getSupportedMobileLinks();

        /**
         * If a deep link is opened from within the Pocket, from Reader or from a context like
         * Saves, where by default we just open the link in Reader, use this method.
         * <p>
         * In this case if it is a deep link that would open Reader anyway, or if it's okay
         * to just load it in Reader, it will return a null intent to allow opening in Reader.
         */
        public static Intent parseLinkOpenedInReader(Context context, String url, ActionContext cxt) {
            return parse(context, url, cxt, Source.Reader);
        }

        /**
         * If a deep link is opened from within the Pocket app and it is considered safe to open
         * whatever it is, use this method.
         */
        public static Intent parseLinkOpenedInPocket(Context context, String url, ActionContext cxt) {
            return parse(context, url, cxt, Source.Internal);
        }

        /**
         * If the deep link is opened from an external application or an unverified source, use this
         * method. Some deep links may be blocked or supported in this mode for security.
         */
        public static Intent parseLinkFromExternalApplication(Context context, String url, ActionContext cxt) {
            return parse(context, url, cxt, Source.External);
        }

        private static Intent parse(Context context, String url, ActionContext cxt, Source source) {
            Uri uri = Uri.parse(url);
            if (isPocketWebUrl(uri)) {
                List<String> segments = uri.getPathSegments();

                // Check for intercepting flag.
                if ("1".equals(uri.getQueryParameter("no_app_intercept"))) {
                    return null;
                }

                for (Link type : SUPPORTED_LINKS) {
                    Intent intent = type.toIntent(context, uri, segments, cxt, source);
                    if (intent != null) {
                        return intent;
                    }
                }
            }
            if (isPocketMobileUrl(uri)) {
                // For pocket:// also add host as a path segment,
                // because we don't really have a host in this case
                var pathSegments = uri.getPathSegments();
                var segments = new ArrayList<String>(pathSegments.size() + 1);
                segments.add(uri.getHost());
                segments.addAll(pathSegments);
                for (Link type : SUPPORTED_MOBILE_LINKS) {
                    Intent intent = type.toIntent(context, uri, segments, cxt, source);
                    if (intent != null) {
                        return intent;
                    }
                }
            }
            return null;
        }

        public static boolean isPocketWebUrl(String url) {
            if (url == null) {
                return false;
            }
            try {
                return isPocketWebUrl(Uri.parse(url));
            } catch (Throwable t) {
                return false;
            }
        }

        public static boolean isPocketWebUrl(Uri uri) {
            if (uri == null) return false;

            return StringUtils2.equalsIgnoreCaseOneOf(uri.getHost(), "getpocket.com", "pocket.co")
                && StringUtils2.equalsIgnoreCaseOneOf(uri.getScheme(), "http", "https");
        }

        public static boolean isPocketMobileUrl(Uri url) {
            return url.getScheme().equals("pocket");
        }

        /**
         * Is this url a variant of getpocket.com/save?url= which is used on the web to save a link to Pocket?
         */
        public static boolean isPocketSaveUrl(@Nullable String url) {
            if (url == null) {
                return false;
            }

            try {
                Uri uri = Uri.parse(url);
                boolean isSaveUrl = uri.getPathSegments().size() == 1
                        && StringUtils2.equalsIgnoreCaseOneOf(uri.getLastPathSegment(), "save", "edit", "save.php", "edit.php")
                        && DeepLinks.Parser.isPocketWebUrl(uri);

                if (isSaveUrl) {
                    // Check for the flag that requests the Android app to pass this VIEW onto the browser instead of handling the save.
                    if (PocketUrlHandlerActivity.isInterceptingDisabled(uri)) {
                        // This url should be treated like any other url and not intercepted for saving.
                        return false;

                    } else {
                        return true;
                    }

                } else {
                    return false;
                }

            } catch (Throwable t) {
                // Not a valid url
                Logs.printStackTrace(t);
                return false;
            }
        }

        public static boolean isReaderPath(String path) {
            return "/app/read".equalsIgnoreCase(path) || "/a/read".equalsIgnoreCase(path);
        }

        private static boolean supported(Context context) {
            return App.from(context).pktcache().isLoggedIn();
        }

        private static Set<Link> getSupportedLinks() {
            Set<Link> links = new HashSet<>();

            // MainActivity destinations
            links.add(new SimplePathLink("saves", DeepLinks::newSavesIntent));
            links.add(new SimplePathLink("home", DeepLinks::newHomeIntent));
            // Settings link is in getSupportedMobileLinks()

            // Premium
            links.add(new SimplePathLink("premium", (context, uiContext) -> App.from(context).pktcache().hasPremium() ? DeepLinks.newPremiumSettingsIntent(context) : DeepLinks.newPremiumIntent(context)));
            links.add(new SimplePathLink("premium_settings", (context, uiContext) -> DeepLinks.newPremiumSettingsIntent(context)));

            // System Settings
            links.add(new SimplePathLink(
                    "settings",
                    "notifications",
                    (context, uiContext) -> DeepLinks.newDeviceNotificationSettingsIntent(context)
            ));

            // /app/*
            // We also support /a/*
            links.add(new SimplePathLink("app", DeepLinks::newOpenAppIntent));
            links.add(new SimplePathLink("a", DeepLinks::newOpenAppIntent));
            links.add(new SimplePathLink("app", "list", DeepLinks::newSavesIntent));
            links.add(new SimplePathLink("a", "list", DeepLinks::newSavesIntent));
            links.add(new SimplePathLink("app", "listen", DeepLinks::newListenIntent));
            links.add(new SimplePathLink("a", "listen", DeepLinks::newListenIntent));

            // Open an Explore Topic Page, eg: https://getpocket.com/explore/science
            links.add((context, uri, pathSegments, cxt, source) -> {
                if (!supported(context)) return null;
                if (pathSegments.size() != 2) return null;
                if (!"explore".equals(pathSegments.get(0))) return null;

                return DeepLinks.newTopicDetailsIntent(context, pathSegments.get(1));
            });

            // Open a Syndicated Article, eg: https://getpocket.com/explore/item/the-rise-and-fall-of-the-bombshell-bandit
            links.add((context, uri, pathSegments, cxt, source) -> {
                if (!supported(context)) return null;
                if (pathSegments.size() != 3) return null;
                if (!"explore".equals(pathSegments.get(0))) return null;
                if (!"item".equals(pathSegments.get(1))) return null;

                // Allow opening directly in Reader.
                if (source == Parser.Source.Reader) return null;

                return DeepLinks.newReaderIntentFromTrustedUrl(uri.toString(), context);
            });

            // Editorial collection, ex: https://getpocket.com/collections/my-joy-list-mary-h-k-choi
            links.add((context, uri, pathSegments, cxt, source) -> {
                // Always supported, don't need to be logged in, don't need to restrict to guests.

                // Check expected url format.
                if (pathSegments.size() != 2) return null;
                if (!"collections".equals(pathSegments.get(0))) return null;

                // Allow opening directly in Reader.
                if (source == Parser.Source.Reader) return null;

                // Create an intent that will open a new Reader instance.
                return DeepLinks.newReaderIntentFromTrustedUrl(uri.toString(), context);
            });

            // "Old style" pocket.co short links. Open the wrapped URL in Reader.
            links.add((context, uri, pathSegments, cxt, source) -> {
                if (!supported(context)) {
                    // Only allow when logged in.
                    return null;
                }
                if (source == Source.Reader) {
                    // Allow opening this in Reader.
                    return null;
                }
                if (!uri.getHost().equals("pocket.co")) return null;
                if (pathSegments.size() != 1) return null;
                if (StringUtils.isBlank(pathSegments.get(0))) return null;

                return DeepLinks.newReaderIntentFromTrustedUrl(uri.toString(), context);
            });

            // Pocket Share Links, e.g. https://pocket.co/share/3a3c1727-caae-4adc-8062-e31dd0ebf7a8
            links.add((context, uri, pathSegments, cxt, source) -> {
                if (!supported(context)) {
                    // Only allow when logged in.
                    return null;
                }

                // Check expected url format.
                if (!"pocket.co".equals(uri.getHost())) return null;
                if (pathSegments.size() != 2) return null;
                if (!"share".equals(pathSegments.get(0))) return null;

                // Create an intent that will open a new Reader instance.
                return DeepLinks.newReaderIntentFromTrustedUrl(uri.toString(), context);
            });

            // Listen
            // Play article in Listen, e.g. pocket://listen?url=https%3A%2F%2Fblog.getpocket.com%2F2023%2F01%2Flatest-pocket-android-app-makes-it-easier-to-discover-your-saved-and-new-stories%2F
            links.add((context, uri, pathSegments, cxt, source) -> {
                if (!supported(context)) return null;
                if (pathSegments.size() != 1) return null;
                if (!pathSegments.get(0).equals("listen")) return null;

                var url = uri.getQueryParameter("url");
                if (url != null) {
                    // Play the specific article.
                    return DeepLinks.newListenIntent(url, context);
                } else {
                    // Play the playlist from beginning.
                    return DeepLinks.newListenIntent(context, null);
                }
            });

            return links;
        }

        private static Set<Link> getSupportedMobileLinks() {
            Set<Link> links = new HashSet<>();

            // Main navigation.
            links.add(new SimplePathLink("settings", DeepLinks::newSettingsIntent));

            return links;
        }

        public interface Link {
            /**
             * Check if this url is a match to this link type. If supported, return an Intent to open.
             * Otherwise return null.
             *
             * @param source For security, consider whether or not this link should be allowed to open if it came from {@code External} source or other app.
             */
            Intent toIntent(Context context, Uri uri, List<String> pathSegments, ActionContext cxt, Source source);
        }
        
        /**
         * A Link that must match a specific static path exactly. Used for links that don't have dynamic data in them.
         */
        private static class SimplePathLink implements Link {

            private final String[] mPath;
            private final IntentCreator mCreator;
    
            /**
             * @param entirePath The path must only have one segment and it must be this.
             */
            private SimplePathLink(String entirePath, IntentCreator creator) {
                mPath = new String[]{entirePath};
                mCreator = creator;
            }

            /**
             * The path must only have these two exact paths
             */
            private SimplePathLink(String first, String second, IntentCreator creator) {
                mPath = new String[]{first, second};
                mCreator = creator;
            }

            @Override
            public Intent toIntent(Context context, Uri uri, List<String> pathSegments, ActionContext cxt, Source source) {
                if (!supported(context)) {
                    return null;
                }
                if (pathSegments.size() != mPath.length) {
                    return null;
                }

                for (int i = 0; i < pathSegments.size(); i++) {
                    if (!StringUtils.equalsIgnoreCase(pathSegments.get(i), mPath[i])) {
                        return null;
                    }
                }

                return mCreator.makeIntent(context, cxt);
            }

            interface IntentCreator {
                Intent makeIntent(Context context, ActionContext cxt);
            }
        }
    }
}
