package com.pocket.sdk.api.endpoint;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pocket.sdk.network.eclectic.EclecticHttp;
import com.pocket.sdk.network.eclectic.EclecticHttpRequest;
import com.pocket.sync.source.Remote;
import com.pocket.util.java.JsonUtil;

import java.util.HashMap;
import java.util.Map;

public class AdzerkEndpoint {
	private static final ObjectNode EMPTY_RESULT = JsonUtil.newObjectNode();
	
	/**
	 * Executes a request to Adzerk and parses and returns the response as a json object.
	 *
	 * @param request The contents of the request
	 * @param httpClient The http client to invoke the request on
	 * @return The response as a json object
	 */
	public static ObjectNode execute(Request request, EclecticHttp httpClient) throws AdzerkApiException {
		try {
			final EclecticHttpRequest httpRequest = httpClient.buildRequest(request.endpointUrl);
			httpRequest.setHeader("User-Agent", request.userAgent);
			if (request.json != null) {
				httpRequest.setJson(request.json.toString());
			}
			for (Map.Entry<String, String> entry : request.parameters.entrySet()) {
				httpRequest.appendQueryParameter(entry.getKey(), entry.getValue());
			}

			if (request.method == Remote.Method.GET) {
				EclecticHttp.Response response = httpClient.get(httpRequest, null);
				if (response.getStatusCode() != 200) {
					throw new AdzerkApiException(null, response.getStatusCode());
				}
				return EMPTY_RESULT;
				
			} else if (request.method == Remote.Method.POST) {
				final Object result = httpClient.post(httpRequest, (stream, response) -> {
					if (response.getStatusCode() != 200) {
						throw new AdzerkApiException(null, response.getStatusCode());
					}
					return JsonUtil.getObjectMapper().readTree(stream.inputStream());
				}).getResponse();
				return (ObjectNode) result;
				
			} else {
				throw new AssertionError("Request method not implemented");
			}
			
		} catch (AdzerkApiException e) {
			throw e;
			
		} catch (Throwable t) {
			throw new AdzerkApiException(t);
		}
	}
	
	public static class Request {
		private final String endpointUrl;
		private final Map<String, String> parameters = new HashMap<>();
		private Remote.Method method;
		private String userAgent;
		private ObjectNode json;

		public Request(String endpointUrl) {
			this.endpointUrl = endpointUrl;
		}
		
		public Request method(Remote.Method method) {
			this.method = method;
			return this;
		}
		
		public Request userAgent(String userAgent) {
			this.userAgent = userAgent;
			return this;
		}
		
		public Request json(ObjectNode json) {
			this.json = json;
			return this;
		}

		public Request addParam(String key, String value) {
			if (value != null) parameters.put(key, value);
			return this;
		}
	}
}
