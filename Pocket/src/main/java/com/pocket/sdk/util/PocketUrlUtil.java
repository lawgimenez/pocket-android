package com.pocket.sdk.util;

import android.net.Uri;

import com.pocket.sdk.api.generated.thing.Post;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import okio.ByteString;

/**
 * TODO Documentation
 */
public class PocketUrlUtil {

    private static final String SALT = "d4f31f7dbc57f2633b347e1c";

    /** Makes getpocket.com/redirect urls */
    public static Uri asRedirect(String url, boolean asTrusted) {
        Uri.Builder builder = new Uri.Builder()
                .scheme("https")
                .authority("getpocket.com")
                .path("redirect")
                .appendQueryParameter("url", url);

        if (asTrusted) {
            String salt = SALT + "0f2eaa6ad4261f795913d2";
            String combo = Uri.encode(url) + "@" + (salt + "e7b4184f00cfd6cac2");
            String hash = ByteString.of(combo.getBytes()).sha256().hex();
            builder.appendQueryParameter("h", hash);
        }

        return builder.build();
    }

    /**
     * @param url
     * @return boolean whether the url is a Pocket explore article.
     */
    public static boolean isExploreUrl(String url) {
        try {
            URI uri = new URI(url);
            String authority = uri.getAuthority();
            String path = uri.getPath();
            if (authority == null || path == null) {
                return false;
            }
            return (authority.equals("getpocket.com") ||
                    authority.endsWith(".getpocket.dev"))
                            && path.startsWith("/explore/item/");
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Append query parameters to the given url
     *
     * @param url
     * @param parameters a {@link Map} of url parameters.
     * @return new String url with the given parameters added.
     */
    public static String appendParams(String url, Map<String, String> parameters) {
        try {
            URI uri = new URI(url);
            StringBuilder queryBuilder = new StringBuilder();

            if (uri.getQuery() != null)
                queryBuilder.append(uri.getQuery());

            for (Map.Entry<String, String> entry: parameters.entrySet()) {
                String param = entry.getKey() + "=" + entry.getValue();
                if (!queryBuilder.toString().isEmpty()) {
                    queryBuilder.append("&");
                }
                queryBuilder.append(param);
            }

            return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), queryBuilder.toString(), uri.getFragment()).toString();
        } catch (URISyntaxException e) {
            return url;
        }
    }
}
