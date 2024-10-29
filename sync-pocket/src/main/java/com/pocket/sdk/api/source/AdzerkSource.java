package com.pocket.sdk.api.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pocket.sdk.api.endpoint.AdzerkApiException;
import com.pocket.sdk.api.endpoint.AdzerkEndpoint;
import com.pocket.sdk.api.endpoint.Endpoint;
import com.pocket.sdk.api.generated.PocketRemoteStyle;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sdk.network.eclectic.EclecticHttp;
import com.pocket.sync.action.Action;
import com.pocket.sync.source.FullResultSource;
import com.pocket.sync.source.JsonConfig;
import com.pocket.sync.source.LimitedSource;
import com.pocket.sync.source.Remote;
import com.pocket.sync.source.Source;
import com.pocket.sync.source.result.Status;
import com.pocket.sync.source.result.SyncResult;
import com.pocket.sync.spec.Syncable;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.value.Allow;

import java.util.Iterator;
import java.util.Map;

import static com.pocket.sdk.api.endpoint.AdzerkApiExceptionKt.unwrapAdzerkApiException;

/**
 * Adzerk's API (specifically Pocket's api with them) as a {@link Source}, supports actions and things of {@link PocketRemoteStyle#ADZERK}.
 * <p>
 * Be sure to set a valid user agent via {@link #setUserAgent(String)} before using.
 *
 * @see PocketRemoteSource for support of all of Pocket's things and actions.
 */
public class AdzerkSource implements FullResultSource, LimitedSource {

    public static final JsonConfig JSON_CONFIG = new JsonConfig(PocketRemoteStyle.ADZERK, true);

    private final EclecticHttp httpClient;
    private String userAgent;

    public AdzerkSource(EclecticHttp httpClient) {
        this.httpClient = httpClient;
    }

    public synchronized void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    public <T extends Thing> SyncResult<T> syncFull(T thing, Action... actions) {
        SyncResult.Builder<T> sr = new SyncResult.Builder<>(thing, actions);

        if (actions.length > 0) {
            for (Action action : actions) {
                if (!isSupported(action)) {
                    sr.action(action, Status.IGNORED, null, null);
                    continue;
                }
                try {
                    execute(action);
                    sr.action(action, Status.SUCCESS, null, null);

                } catch (Throwable t) {
                    AdzerkApiException apie = unwrapAdzerkApiException(t);
                    Status status;
                    if (apie == null) {
                        // Can't be sure if this is a retryable error or not, so default to retryable.
                        status = Status.FAILED;
                    } else if (apie.getHttpStatusCode() > 0) {
                        status = Status.IGNORED;
                    } else {
                        status = Status.FAILED;
                    }
                    sr.action(action, status, t, null);
                }
            }
            if (sr.hasFailures()) return sr.build();
        }

        if (thing != null) {
            if (!isSupported(thing)) {
                sr.thing(Status.IGNORED, null, null);
            } else {
                try {
                    ObjectNode adzerkRes = execute(thing);
                    T t = (T) thing.getCreator().create(adzerkRes, JSON_CONFIG, Allow.UNKNOWN);
                    t = (T) t.builder().set(thing.identity()).build();
                    sr.thing(t);
                } catch (Throwable e) {
                    sr.thing(Status.FAILED, e, null);
                }
            }
        }

        return sr.build();
    }

    private ObjectNode execute(Syncable definition) throws AdzerkApiException {
        Remote.Method method = definition instanceof Thing ? Remote.Method.POST : Remote.Method.GET;
        Remote.RemoteCallDetails details = Remote.prepare(definition, JSON_CONFIG);
        PocketRemoteSource.cleanupRemoteCallDetails(details);
        AdzerkEndpoint.Request request = new AdzerkEndpoint.Request(details.path)
                .userAgent(userAgent);

        request.method(method);
        switch (method) {
            case GET:
                Iterator<Map.Entry<String, JsonNode>> it = details.remainingFieldsAliased.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> e = it.next();
                    String key = e.getKey();
                    JsonNode value = e.getValue();
                    if (value != null && !value.isNull()) {
                        request.addParam(key, value.isObject() || value.isArray() ? value.toString() : value.asText());
                    }
                }
                break;
            case POST:
                var json = details.remainingFieldsAliased;
                fixDecisionApiRequest(json);
                request.json(json);
                break;
        }
        var response = AdzerkEndpoint.execute(request, httpClient);
        fixDecisionApiResponse(response);
        return response;
    }

    private void fixDecisionApiRequest(ObjectNode json) {
        // Add the required `placements` array.
        var placements = json.putArray("placements");
        
        var placement = (ObjectNode) json.remove("placement");
        if (placement != null) {
            // Set a default value of 1 for `count`. If we don't declare it then Adzerk returns
            // only 1 decision, so it's functionally the same. But they also change the shape of
            // the response. So to keep things consistent, we want to always declare it.
            if (!placement.has("count")) {
                placement.put("count", 1);
            }

            // Move our simplified single `placement` into the array.
            placements.add(placement);
        }
    }

    private void fixDecisionApiResponse(ObjectNode json) {
        // Adzerk returns decision arrays nested in an object, one field for each placement
        // in the request. Since we always request decisions for a single placement, we can
        // remove one level of nesting and extract it to a top level array.
        var decisionsObject = (ObjectNode) json.remove("decisions");
        var decisionsArrayOrNull = decisionsObject.fields().next().getValue();
        json.set("decisions", decisionsArrayOrNull);

        // ctFull<something>path fields work by concatenating Adzerk's base asset path
        // with the file name which is also present in ct<Something> field.
        // This concatenation sometimes breaks and they just return the base path.
        // In this case let's try to workaround by manually appending the file name.
        try {
            var iterator = decisionsArrayOrNull.elements();
            while (iterator.hasNext()) {
                var decision = iterator.next();
                var data = ((ObjectNode) decision.get("contents").get(0).get("data"));
                fixMissingFileName(data, "ctFullimagepath", "ctImage");
                fixMissingFileName(data, "ctFullLogopath", "ctLogo");
            }
        } catch (Throwable ignored) {
            // Don't crash if the workaround doesn't work.
        }

        // Inject our local `received_at` field.
        json.put("received_at", Timestamp.now().seconds());
    }

    /** If full path doesn't contain file name, try to fix by appending the file name. */
    private void fixMissingFileName(ObjectNode data, String fullPathField, String fileNameField) {
        var fullPath = data.get(fullPathField).asText();
        var fileName = data.get(fileNameField).asText();
        if (!fullPath.contains(fileName)) {
            data.put(fullPathField, fullPath + fileName);
        }
    }

    @Override
    public boolean isSupported(Syncable syncable) {
        Remote remote = syncable.remote();
        if (remote == null) return false;
        return remote.style == PocketRemoteStyle.ADZERK;
    }
}
