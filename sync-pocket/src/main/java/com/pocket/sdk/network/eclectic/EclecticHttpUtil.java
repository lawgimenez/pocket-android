package com.pocket.sdk.network.eclectic;


import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public abstract class EclecticHttpUtil {

    /**
     * Convenience method for obtaining the content of a url as a string.
     * @param request The request to make as a GET request
     * @return The content or null if there was an error or it returned anything other than a 200 status.
     * @see #postString(EclecticHttpRequest, EclecticHttp) for POST requests
     */
    public static String getString(EclecticHttpRequest request, EclecticHttp client) {
        try {
            return (String) client.get(request, (in, res) -> {
                if (res.getStatusCode() != 200) return null;
                ContentType type = getContentType(res);
                String charset = type != null && type.encoding != null ? type.encoding : "UTF-8";
                return in.okioBuffer().readString(Charset.forName(charset));
            }).getResponse();
        } catch (Exception ignored) {
            return null;
        }
    }
    
    /**
     * Convenience method for obtaining the content of a url as a string.
     * @param request The request to make as a POST request
     * @return The content or null if there was an error or it returned anything other than a 200 status.
     * @see #getString(EclecticHttpRequest, EclecticHttp) for GET requests
     */
    public static String postString(EclecticHttpRequest request, EclecticHttp client) {
        try {
            return (String) client.post(request, (in, res) -> {
                if (res.getStatusCode() != 200) return null;
                ContentType type = getContentType(res);
                String charset = type != null && type.encoding != null ? type.encoding : "UTF-8";
                return in.okioBuffer().readString(Charset.forName(charset));
            }).getResponse();
        } catch (Exception ignored) {
            return null;
        }
    }
    
    /**
     * Helper for extracting the mimeType and encoding of a response.
     * @return The content type or null if it was not present in the headers.
     */
    public static ContentType getContentType(EclecticHttp.Response response) {
        String contentType = response.getHeader("Content-Type");
        if (contentType != null) { // OPT reuse pattern instance?
            Matcher matcher = Pattern.compile("([a-z\\-\\_]*/[a-z\\-\\_]*)(?:;\\s*?charset=([a-z\\-\\_0-9]*))?", Pattern.CASE_INSENSITIVE).matcher(contentType);
            if (matcher.find()) {
                return new ContentType(matcher.group(1), matcher.group(2));
            }
        }
        return null;
    }
    
    /**
     * Helper for extracting the mimeType from a response.
     * @return The mimeType or null if it was not present in the headers.
     * @see #getContentType(EclecticHttp.Response)
     */
    public static String getMimeType(EclecticHttp.Response response) {
        ContentType type = getContentType(response);
        return type != null ? type.mimeType : null;
    }
    
    /**
     * Helper for extracting content length as a byte length
     * @return the byte count from the content length header or -1 if it couldn't be read
     */
    public static long getContentLength(EclecticHttp.Response res) {
        try {
            String v = res.getHeader("Content-Length");
            return v != null ? Long.parseLong(v) : -1;
        } catch (Throwable t) {
            return -1;
        }
    }
    
    public static class ContentType {
        public final String mimeType;
        public final String encoding;
        public ContentType(String mimeType, String encoding) {
            this.mimeType = StringUtils.trimToNull(mimeType);
            this.encoding = StringUtils.trimToNull(encoding);
        }
    }

}
