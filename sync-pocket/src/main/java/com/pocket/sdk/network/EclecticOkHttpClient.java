package com.pocket.sdk.network;

import com.pocket.sdk.network.eclectic.EclecticHttp;
import com.pocket.sdk.network.eclectic.EclecticHttpRequest;
import com.pocket.sdk.network.eclectic.KeyFileValue;
import com.pocket.sdk.network.eclectic.KeyValue;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.JavaNetCookieJar;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.BufferedSource;

/**
 * An {@link EclecticHttp} powered by OkHttp.
 */
public class EclecticOkHttpClient implements EclecticHttp {

    private final OkHttpClient mClient;

    /** Increments each time release() is called so all requests can be cancelled at release() time. */
    private int mReleaseTag = 1;
    private boolean mIsEnabled = true;

    public EclecticOkHttpClient(OkHttpClient client) {
        if (CookieHandler.getDefault() == null) {
            CookieHandler.setDefault(new CookieManager());
        }

        mClient = client.newBuilder()
                .cookieJar(new JavaNetCookieJar(CookieHandler.getDefault()))
                .build();
    }

    private enum Method {
        POST("POST"), DELETE("DELETE"), PATCH("PATCH"), PUT("PUT");
        private final String name;
        Method(String name) {
            this.name = name;
        }
    }

    @Override
    public Response post(EclecticHttpRequest request, ResponseParser parser) throws Exception {
        return execute(request, parser, Method.POST);
    }

    @Override
    public Response delete(EclecticHttpRequest request, ResponseParser parser) throws Exception {
        return execute(request, parser, Method.DELETE);
    }

    /** Executes requests that use a request body. */
    private Response execute(EclecticHttpRequest request, ResponseParser parser, Method method) throws Exception {
        checkEnabled();
        
        Request.Builder okHttpRequestBuilder = new Request.Builder();

        // Headers
        attachHeaders(okHttpRequestBuilder, request);

        // Query
        List<KeyValue> params = request.getParams();
        request.clearQuery(); // Moved query to post body, so clear it from the url. TODO don't do this in this way, it makes the request object change, so callers of this have their object change.

        okHttpRequestBuilder
                .tag(mReleaseTag)
                .url(request.getUrl());

        RequestBody body = body(request, params);
        okHttpRequestBuilder.method(method.name, body);
        return execute(okHttpRequestBuilder.build(), parser);
    }

    private RequestBody body(EclecticHttpRequest request, List<KeyValue> params) {
        // Post body
        if (request.getJson() != null) {
            // JSON body
            final MediaType mediaType = MediaType.parse("application/json");
            return RequestBody.create(request.getJson(), mediaType);
    
            // TODO what if someone adds JSON and files and params? throw? or figure out how to support?
        } else if (!request.getFiles().isEmpty()) {
            // Multipart

            MultipartBody.Builder multiBuilder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM);

            // Files
            List<KeyFileValue> files = request.getFiles();
            for (KeyFileValue file : files) {
                Map<String, String> headerMap = new HashMap<>();
                headerMap.put("Content-Disposition", "form-data;" +
                        " " +
                        "name=\"" + file.key + "\";" +
                        " " +
                        "filename=\"" + file.value.getName() + "\"");
                // Content-Type: application/octet-stream, Content-Transfer-Encoding: binary"),
                Headers headers = Headers.of(headerMap);

                MediaType mediaType = null;
                String mimeType = URLConnection.guessContentTypeFromName(file.value.getAbsolutePath());
                if (mimeType != null) {
                    mediaType = MediaType.parse(mimeType);
                }

                multiBuilder.addPart(headers, RequestBody.create(file.value, mediaType));
            }

            // Query
            for (KeyValue param : params) {
                multiBuilder.addFormDataPart(param.key, param.value);
            }

            return multiBuilder.build();

        } else {
            // Encoded query only
            FormBody.Builder queryBuilder = new FormBody.Builder();
            for (KeyValue param : params) {
                queryBuilder.add(param.key, param.value);
            }
            return queryBuilder.build();
        }
    }

    @Override
    public Response get(EclecticHttpRequest request, ResponseParser parser) throws Exception {
        checkEnabled();
        
        Request.Builder okHttpRequestBuilder = new Request.Builder();
        attachHeaders(okHttpRequestBuilder, request);
        Request okRequest = okHttpRequestBuilder
                .url(request.getUrl())
                .tag(mReleaseTag)
                .build();

        return execute(okRequest, parser);
    }

    @Override
    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;
    }
    
    private void checkEnabled() {
        if (!mIsEnabled) {
            throw new RuntimeException("Network disabled");
        }
    }
    
    private void attachHeaders(Request.Builder okHttpRequestBuilder, EclecticHttpRequest request) {
        Set<KeyValue> headers = request.getHeaders();
        for (KeyValue header : headers) {
            if (StringUtils.equalsIgnoreCase(header.value, "gzip") && StringUtils.equalsIgnoreCase(header.key, "Accept-Encoding")) {
                continue; // OkHttp handles gzip by default and has a bug if you declare it manually where some cases won't ungzip https://github.com/square/okhttp/issues/1927
            }
            okHttpRequestBuilder.header(header.key, header.value);
        }
    }
    
    private Response execute(Request request, ResponseParser parser) throws Exception {
        okhttp3.Response okResponse = mClient.newCall(request).execute();
        try {
            final OkResponseWrapper result = new OkResponseWrapper(okResponse);
            if (parser != null) {
                result.mData = parser.readResponse(new Stream() {
                    boolean used;
                    private void used() {
                        if (used) throw new RuntimeException("stream already used");
                        used = true;
                    }
                    @Override
                    public InputStream inputStream() {
                        used();
                        return okResponse.body().byteStream();
                    }
                    @Override
                    public BufferedSource okioBuffer() {
                        used();
                        return okResponse.body().source();
                    }
                }, result);
            }
            return result;
        } finally {
            okResponse.close();
        }
    }

    @Override
    public CookieManager getCookieManager() {
        return (CookieManager) CookieHandler.getDefault();
    }

    @Override
    public void release() {
        cancelByTag(mClient.dispatcher().runningCalls(), mReleaseTag);
        cancelByTag(mClient.dispatcher().queuedCalls(), mReleaseTag);
        mReleaseTag++;
    }

    private void cancelByTag(List<Call> calls, int releaseTag) {
        for (Call call : calls) {
            final Object tag = call.request().tag();
            if (tag != null && tag.equals(releaseTag)) {
                call.cancel();
            }
        }
    }

    private static class OkResponseWrapper implements Response {

        private final int mStatusCode;
        private final Headers mHeaders;
        private final String mEndUrl;
    
        private Object mData;

        private OkResponseWrapper(okhttp3.Response res) {
            mStatusCode = res.code();
            mHeaders = res.headers();
            mEndUrl = res.request().url().toString();
        }

        @Override
        public int getStatusCode() {
            return mStatusCode;
        }

        @Override
        public Object getResponse() {
            return mData;
        }

        @Override
        public String getHeader(String name) {
            return mHeaders.get(name);
        }
    
        @Override
        public String endUrl() {
            return mEndUrl;
        }
    }

    @Override
    public EclecticHttpRequest buildRequest(String url) {
        HttpUrl request = HttpUrl.parse(url);
        if (request == null) throw new RuntimeException("Could not parse " + url);
        return new EclecticHttpRequest() {
    
            private final HttpUrl.Builder builder = request.newBuilder();
            private final Set<KeyValue> headers = new HashSet<>();
            private final List<KeyFileValue> files = new ArrayList<>();
            private String json;
            
            @Override
            public EclecticHttpRequest appendQueryParameter(String key, String value) {
                builder.addQueryParameter(key, value);
                return this;
            }
    
            @Override
            public EclecticHttpRequest addFile(String name, File file) {
                files.add(new KeyFileValue(name, file));
                return this;
            }
    
            @Override
            public EclecticHttpRequest setHeader(String name, String value) {
                headers.add(new KeyValue(name, value));
                return this;
            }
    
            @Override public EclecticHttpRequest setJson(String body) {
                json = body;
                return this;
            }
    
            @Override
            public EclecticHttpRequest clearQuery() {
                builder.query(null);
                return this;
            }
    
            @Override
            public String getUrl() {
                return builder.toString();
            }
    
            @Override
            public List<KeyValue> getParams() {
                List<KeyValue> keyvals = new ArrayList<>();
                HttpUrl built = builder.build();
                for (String name : built.queryParameterNames()) {
                    keyvals.add(new KeyValue(name, built.queryParameter(name)));
                }
                return keyvals;
            }
    
            @Override
            public List<KeyFileValue> getFiles() {
                return files;
            }
    
            @Override
            public Set<KeyValue> getHeaders() {
                return headers;
            }
    
            @Override
            public String getPath() {
                return builder.build().encodedPath();
            }
    
            @Override
            public String getJson() {
                return json;
            }
        };
    }
}
