package com.pocket.sdk.api.endpoint;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pocket.sdk.network.eclectic.EclecticHttp;
import com.pocket.sdk.network.eclectic.EclecticHttpRequest;
import com.pocket.util.java.JsonUtil;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okio.ByteString;


/**
 * Stateless execution of a request to the Pocket V3 Api.
 * Essentially just a helper to take the various parts of a {@link Request} and apply them to an http call formatted the way v3 expects.
 * To use, see {@link #execute(Request, EclecticHttp)}
 */
public class Endpoint {
	
	private static final String USER_AGENT_DELIM = ";";
	
	/**
	 * Executes a request to v3 and parses and returns the response as a json object.
	 * This is the most common use case. If more control over the response is needed see {@link #execute(Request, EclecticHttp, ResponseStreamer)}.
	 *
	 * @param request The contents of the request
	 * @param httpClient The http client to invoke the request on
	 * @return The response as a json object
	 * @throws ApiException
	 */
	public static ObjectNode execute(Request request, EclecticHttp httpClient) throws ApiException {
		return (ObjectNode) execute(request, httpClient, inputStream -> JsonUtil.getObjectMapper().readTree(inputStream));
	}
	
	/**
	 * Executes a request to v3.
	 *
	 * @param request The contents of the request
	 * @param httpClient The http client to invoke the request on
	 * @param responseStreamer How to handle/parse the response. Optional.
	 * @return The response of the responseStreamer
	 * @throws ApiException
	 */
	public static Object execute(Request request, EclecticHttp httpClient, final ResponseStreamer responseStreamer) throws ApiException {
		try {
			EclecticHttpRequest httpRequest = httpClient.buildRequest(request.endpointUrl);
			httpRequest.setHeader("X-Accept", "application/json");
			httpRequest.setHeader("Accept-Encoding", "gzip");
			httpRequest.setHeader("User-Agent", userAgent(request.app, request.device));
			if (request.device.userAgent != null) {
				httpRequest.setHeader("X-Device-User-Agent", request.device.userAgent); // TODO document in Spec repo, but this should be a representation of a typical browser agent from your device. Used for spoc tracking on the backend https://pocket.slack.com/archives/C03C9QQE0/p1503082531000444?thread_ts=1503081169.000131&cid=C03C9QQE0
			}
			httpRequest.setHeader("Accept-Language", request.device.locale);
			httpRequest.appendQueryParameter("locale_lang", request.device.locale);
			httpRequest.appendQueryParameter("consumer_key", request.app.consumerKey);
			if (request.guid != null) {
				httpRequest.appendQueryParameter("guid", request.guid);
			}
			if (request.accessToken != null) {
				httpRequest.appendQueryParameter("access_token", request.accessToken);
			}
			if (request.accessToken != null || request.hashTarget != null) {
				String timestamp = String.valueOf(System.currentTimeMillis());
				String nonce = RandomStringUtils.randomAlphanumeric(16);
				String hashTarget = request.hashTarget != null ? request.hashTarget : request.accessToken;
				httpRequest.appendQueryParameter("oauth_timestamp", timestamp);
				httpRequest.appendQueryParameter("oauth_nonce", nonce);
				httpRequest.appendQueryParameter("sig_hash", hash(timestamp, nonce, hashTarget));
			}
			for (Map.Entry<String, String> entry : request.parameters.entrySet()) {
				httpRequest.appendQueryParameter(entry.getKey(), entry.getValue());
			}
			for (Map.Entry<String, File> entry : request.files.entrySet()) {
				httpRequest.addFile(entry.getKey(), entry.getValue());
			}

			EclecticHttp.Response response = httpClient.post(httpRequest, (inputStream, response1) -> {
				if (response1.getStatusCode() != 200) {
					throw newExceptionFromResponse(response1, httpRequest);
				}
				if (responseStreamer != null) {
					return responseStreamer.readResponse(inputStream.inputStream());
				} else {
					return true;
				}
			});

			int httpStatusCode = response.getStatusCode();
			if (httpStatusCode == 200) {
				return response.getResponse();
			} else {
				throw newExceptionFromResponse(response, httpRequest);
			}

		} catch (ApiException e) {
			throw e;

		} catch (Throwable t) {
			throw new ApiException(ApiException.Type.CONNECTION, t, 0, null, null, null);
		}
	}
	
	public interface ResponseStreamer {
		/**
		 * Process the InputStream of the response and return a result. If any problems are encountered, throw an exception.
		 * You do not need to worry about closing the stream.
		 *
		 * @param inputStream The response as an InputStream
		 * @return The result to be returned as the result
		 * @throws Exception Throw exceptions as needed
		 */
		Object readResponse(InputStream inputStream) throws Exception;
	}

	private static String getXHeaderValue(String name, EclecticHttp.Response response) {
		return StringUtils.trimToNull(
				StringUtils.replaceOnce(response.getHeader(name), name+":", ""));
	}

	public static String hash(String timestamp, String nonce, String value) {
		String toHash = timestamp + nonce + value + EndpointStrings.API_REQUEST_SALT;
		return ByteString.of(toHash.getBytes()).md5().hex();
	}

	private static ApiException newExceptionFromResponse(EclecticHttp.Response response, EclecticHttpRequest request) {
		int httpStatusCode = response.getStatusCode();
		String xError = StringEscapeUtils.unescapeJava(Endpoint.getXHeaderValue("X-Error", response)); // The server will returned escaped characters, so we need to unescape them before displaying them to a user.
		String xErrorCode = Endpoint.getXHeaderValue("X-Error-Code", response);
		String xErrorData = Endpoint.getXHeaderValue("X-Error-Data", response);
		ApiException.Type type;
		if (httpStatusCode == 401 && a401OnThisEndpointMeanRevokedToken(request.getPath())) {
			type = ApiException.Type.POCKET_ACCESS_TOKEN_REVOKED;
		} else {
			type = ApiException.Type.POCKET;
		}
		return new ApiException(type, null, httpStatusCode, xError, xErrorCode, xErrorData);
	}
	
	/**
	 * Is this request being made to an endpoint that returns 401s normally instead of indicating a revoked token?
	 * TODO When the backend changes to use an X-Error-Code we can remove this special logic.
	 * See https://pocket.slack.com/archives/C067Y2396/p1546434942001500
	 */
	private static boolean a401OnThisEndpointMeanRevokedToken(String path) {
		if (path == null) return true;
		if (!path.startsWith("/v3")) return true;
		return !StringUtils.startsWithAny(StringUtils.replaceOnce(path, "/v3/", ""), "acctchange", "addAlias", "deleteAlias", "registerSocialToken", "resendEmailConfirmation", "bookmarklet", "guid", "oauth", "signup", "validateEmail");
	}
	
	public static class Request {
		
		public final String endpointUrl;
		private final Map<String, String> parameters = new HashMap<>();
		private final Map<String, File> files = new HashMap<>();
		private String hashTarget;
		private String guid;
		private String accessToken;
		private DeviceInfo device;
		private AppInfo app;

		public Request(String endpointUrl) {
			this.endpointUrl = endpointUrl;
		}
		
		public Request guid(String guid) {
			this.guid = guid;
			return this;
		}
		
		public Request accessToken(String accessToken) {
			this.accessToken = accessToken;
			return this;
		}
		
		public Request hashTarget(String value) {
			this.hashTarget = value;
			return this;
		}
		
		public Request device(DeviceInfo device) {
			this.device = device;
			return this;
		}
		
		public Request app(AppInfo app) {
			this.app = app;
			return this;
		}

		public Request addParam(String key, String value) {
			if (value != null) {
				parameters.put(key, value);
			}
			return this;
		}

		public Request addParam(String key, int value) {
			return addParam(key, String.valueOf(value));
		}

		public Request addFile(String name, File file) {
			files.put(name, file);
			return this;
		}
	}

	public static String userAgent(AppInfo app, DeviceInfo device) {
		return new StringBuilder()
				.append(app.company).append(USER_AGENT_DELIM)
				.append(app.product).append(USER_AGENT_DELIM)
				.append(app.productVersion).append(USER_AGENT_DELIM)
				.append(device.os).append(USER_AGENT_DELIM)
				.append(device.osVersion).append(USER_AGENT_DELIM)
				.append(device.deviceManufactuer).append(USER_AGENT_DELIM)
				.append(device.deviceModel).append(USER_AGENT_DELIM)
				.append(device.deviceType).append(USER_AGENT_DELIM)
				.append(app.build).append(USER_AGENT_DELIM)
				.append(app.store)
				.toString();
	}

}
