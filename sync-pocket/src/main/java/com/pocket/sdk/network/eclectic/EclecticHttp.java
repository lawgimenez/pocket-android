package com.pocket.sdk.network.eclectic;


import java.io.InputStream;
import java.net.CookieManager;

import okio.BufferedSource;

/**
 * A generic interface for interacting with a Network/Http client.
 * <p>
 * Your app can use this interface and then implementations of this client can
 * be created for different http client libraries. This makes it very easy to swap out
 * or update http clients without having to change your app's code.
 */
public interface EclecticHttp {
    
    /**
     * Parses the url and returns a request object that can be further modified or submitted.
     */
    EclecticHttpRequest buildRequest(String url);

    /**
     * Send a request via POST. All of the params in your requests Uri will be encoded and sent
     * as the POST body. This method supports uploading files.
     */
    Response post(EclecticHttpRequest request, ResponseParser parser) throws Exception;

    /**
     * Access the contents of a url.
     */
    Response get(EclecticHttpRequest request, ResponseParser parser) throws Exception;

    /** Makes a request via DELETE. */
    Response delete(EclecticHttpRequest request, ResponseParser parser) throws Exception;

    CookieManager getCookieManager();

    /**
     * Tell the client it is no longer needed. This will depend on the client but typically
     * this should cancel all pending connections and completely release resources and shut itself
     * down. This may be asynchronous or synchronous.
     */
    void release();
    
    /**
     * Control whether or not new, future network connections are currently allowed.
     * While disabled, exceptions should be thrown for methods like get and post.
     * Defaults to enabled.
     */
    void setEnabled(boolean enabled);

    interface ResponseParser {
        /**
         * Invoked by the client after connecting, provides an InputStream of the response
         * from the server.
         *
         * @param inputStream The content from the request response
         * @param response A response object so you can read status, headers, etc.
         * @return Optionally return a value you want to be available via {@link EclecticHttp.Response#getResponse()} later.
         */
        Object readResponse(Stream inputStream, Response response) throws Exception;
    }
    
    /** Your choice of how to work with the data. Only call one method, it is an error to call more than one. */
    interface Stream {
        InputStream inputStream();
        BufferedSource okioBuffer();
    }
    
    interface Response {
        int getStatusCode();
        Object getResponse();
        String getHeader(String name);
        /** The url that this response came from, this could be different than the requested one if it was redirected. */
        String endUrl();
    }

    enum Logging {
        NONE,
        API,
        EVERYTHING,
    }
}
