package com.pocket.sdk.network.eclectic;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 *  A representation of an http request. Use {@link EclecticHttp#buildRequest(String)} to obtain a new instance.
 */
public interface EclecticHttpRequest {

    EclecticHttpRequest appendQueryParameter(String key, String value);
    EclecticHttpRequest addFile(String name, File file);
    EclecticHttpRequest setHeader(String name, String value);
    EclecticHttpRequest setJson(String body);
    EclecticHttpRequest clearQuery();
    
    String getUrl();
    List<KeyValue> getParams();
    List<KeyFileValue> getFiles();
    Set<KeyValue> getHeaders();
    String getPath();
    String getJson();
}
