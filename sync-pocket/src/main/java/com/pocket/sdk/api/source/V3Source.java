package com.pocket.sdk.api.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pocket.sdk.api.endpoint.ApiException;
import com.pocket.sdk.api.endpoint.Credentials;
import com.pocket.sdk.api.endpoint.Endpoint;
import com.pocket.sdk.api.generated.Modeller;
import com.pocket.sdk.api.generated.PocketAuthType;
import com.pocket.sdk.api.generated.PocketRemoteStyle;
import com.pocket.sdk.api.generated.action.Add;
import com.pocket.sdk.api.generated.action.Purchase;
import com.pocket.sdk.api.generated.action.Readd;
import com.pocket.sdk.api.generated.action.SharedTo;
import com.pocket.sdk.api.generated.thing.ArticleView;
import com.pocket.sdk.api.generated.thing.Get;
import com.pocket.sdk.api.generated.thing.Guid;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.api.generated.thing.SharedToResult;
import com.pocket.sdk.api.value.FileField;
import com.pocket.sdk.network.eclectic.EclecticHttp;
import com.pocket.sync.action.Action;
import com.pocket.sync.source.AuthType;
import com.pocket.sync.source.FullResultSource;
import com.pocket.sync.source.JsonConfig;
import com.pocket.sync.source.LimitedSource;
import com.pocket.sync.source.Remote;
import com.pocket.sync.source.Source;
import com.pocket.sync.source.SynchronousSource;
import com.pocket.sync.source.result.RemotePriority;
import com.pocket.sync.source.result.Status;
import com.pocket.sync.source.result.SyncException;
import com.pocket.sync.source.result.SyncResult;
import com.pocket.sync.spec.Syncable;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.value.Allow;
import com.pocket.sync.value.Include;
import com.pocket.util.java.Safe;
import com.pocket.util.java.StringUtils2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Pocket's v3 API as a {@link Source}, supports actions and things of the following styles:
 * * {@link PocketRemoteStyle#V3}
 * * {@link PocketRemoteStyle#PARSER}
 * <p>
 * Stateless aside from what {@link Credentials} it will use. You must invoke {@link #setCredentials(Credentials)} to setup at least its app and device credentials before invoking sync.
 * <p>
 * If {@link Credentials#getGuid()} is missing and an endpoint that requires a guid is submitted, this will
 * automatically attempt to obtain a guid from v3 and update the {@link Credentials} with the guid.
 * To receive the guid, monitor {@link SyncResult#resolved}, it will appear there if it was obtained.
 *
 * <pre>
 The v3 API is Pocket's main http api. We are currently in the process of replacing it with {CLIENT_API}.
 #
 # ## Endpoint Addresses

 It is made up of various endpoints. To construct the endpoint address, start with the server, which for production is `https://api.getpocket.com/`, then `v3/` and then the endpoint path.

 For example, the `send` endpoint is available at `https://api.getpocket.com/v3/send`.

 Sometimes dev servers are used for pre-production testing.


 ## Endpoint Requests

 ### Request Type

 Use either a `POST` or `GET` request type.

 The default content type for `POST`` is to use `application/x-www-form-urlencoded`. However, `application/json` is also supported.

 ### Request Headers:

 * `Accept-Encoding: gzip`
 * `X-Accept: application/json`
 * `X-Device-User-Agent` with a value of whatever would be a typical browser user agent on the device making this request (Note, this likely is no longer needed)
 * `Accept-Language` with the same value we include in `locale_lang` param (see below)

 Official Pocket Clients should include a custom string for the `User-Agent` header:

 * `User-Agent: Pocket;Free;6.5.6.5;iOS;10.3;Apple;x86_64;Mobile;App Store;Production`

 A User-Agent string is formed by combining the following values with a `;` as a delimiter:

 1. Name of company/app, such as `Pocket`
 2. Product variant of the app, such as `Free`
 3. Current app version, such as `6.5.6.5`
 4. OS such as `Android` or `iOS`
 5. OS Version like `10.3`
 6. Device Manufacturer like `Apple` or `Samsung`
 7. Device Model Name/Model like `Pixel` or `iPhone`
 8. Device Form, either `mobile` or `tablet`
 9. What store the app build was made for like `play` or `App Store`
 10. What store the app was installed from like `play` or `App Store`

 If any values are not known or do not apply to your app or device, leave it blank like `;;`

 If you have any doubt what value to use, check with the backend and analytic teams.

 ### Request Parameters

 Include the following parameters to all requests, in addition to the specific fields for the thing and action:

 * `consumer_key` : The consumer key for your app. See this [doc](https://getpocket.atlassian.net/wiki/spaces/PE/pages/1001816065/Whitelisting+API+Keys+As+Native) for help creating a new key if needed.
 * `locale_lang` : The user's preferred language for any text returned. In a format similar to `en-us`.

 ### Authentication

 If the thing/action you are using is an authenticated endpoint that requires a user's access token, you must include these additional parameters in the request:

 * `access_token` : The access token given from completing a login or signup request.
 * `oauth_timestamp` : A string value of the current time in milliseconds past epoch.
 * `oauth_nonce` : A random 16 digit alphanumeric string. Generated anew for each request.
 * `sig_hash` : A string representation of a md5 hash of concatenating (`oauth_timestamp` + `oauth_nonce` + `access_token` + `salt`). For the `salt`, talk to the backend or security teams to get access.


 ## Endpoint Responses

 First thing to check with the response is the HTTP status code. Here are some specific ones to look for:

 | Status Code | Meaning |
 | ----------- | ------- |
 | 200 | Success, see the [Success Response](#success) |
 | 400 | Invalid request, double check this general spec as well as the spec for the specific endpoint to make sure it is formatted correctly. More specific error messages may be available. See the [Error Response](#errors) for details. |
 | 401 | User Authentication failed, make sure you properly included the Authentication Parameters outlined above. Otherwise this indicates the access token is no longer valid and you should log the user out as your app no longer has permission to access the user's Pocket. More specific error messages may be available. See the [Error Response](#errors) for details. |
 | 403 | Rate limited. There is a per user and a per app rate limit. All official apps should be whitelisted to avoid app limits. See the `X-Limit-` headers in the [Error Response](#errors) for more details on current limits |
 | 503 | Pocket System Maintenance. Try again later. See the [Error Response](#errors) for possible messages to display |
 | Other | See [Error Response](#errors) |

 #### Success

 If you used the recommended `X-Accept: application/json` header, the response body will be a json object with the following fields:

 * `status` : This should be set to `1`. *TODO in what cases would an error be handled here instead of the headers and status code? Is this used by anything? Can we deprecate it?*
 * `error` : An error message if the status is not `1` *TODO should we use this? Is this used by anything? Wouldn't the error be in the X-Error- headers? Can we deprecate it?*

 And then whatever endpoint specific parameters are returned. To know what parameters are supported see the [definition](../../) for the endpoint.

 #### Errors

 For most errors, the following headers will be available to get additional information on what went wrong. If none of these are available then the connection itself failed or there was a deeper server error that should be reported to the server ops team. If you haven't seen it already look at the list of [status codes](#response).

 Header | Description
 ---|---
 X-Error | A user displayable error of what went wrong, localization will be based on the `locale` value you provided in the request
 X-Error-Code | A Pocket error code, this can be displayed to the user so they can report it to us for further debugging
 X-Limit-User-Limit | Current rate limit enforced per user
 X-Limit-User-Remaining | Number of calls remaining before hitting user's rate limit
 X-Limit-User-Reset | Seconds until user's rate limit resets
 X-Limit-Key-Limit | Current rate limit enforced per consumer key (per app)
 X-Limit-Key-Remaining | Number of calls remaining before hitting consumer key's rate limit
 X-Limit-Key-Reset | Seconds until consumer key rate limit resets


 ## Applying Actions

 By default, unless an action definition has an endpoint specified, apps should collect actions and send them to the `send` endpoint in bulk whenever it syncs.

 When logged out, or before login, the `send_guid` endpoint may be used, but it only accepts {pv}, {pv_wt}, and {pv_ab} actions.

 If an action has an endpoint specified, hit the specified endpoint for that action.

 ### `send`

 `https://api.getpocket.com/v3/send`

 Requires user authentication parameters in addition to:

 | Parameter    | Type   | Description |
 |--------------|--------|-------------|
 | actions      | String | A JSON array of actions, encoded as a string. See below for more details on creating action JSON. |
 | guid         | String | The device's current {guid} if known |

 For each action, create a JSON object with a field of each of its inputs

 For example, for an {add} action:

 ```json
 {
   "action": "add",
   "url": "https://www.nasa.gov/image-feature/goddard/2017/hubbles-bright-shining-lizard-star"
 }
 ```

 The endpoint's response has the following fields:

 #### `action_results`

 `action_results` is an array that contains a response for every action that was sent in `actions`, in the same order.

 So the first element in `action_results` is the response to the first action in `actions`,
 the second element in `action_results` is the response to the second action in `actions`, and so on.

 Almost all actions will either have a `true` (if successful) or a `false` (if failed) as its response.
 If `false`, you can reference the [action_errors](#action_errors) field for more details.

 Some actions will return a json object as a success response. This object will contain some information about
 the things related to the action that might be useful for the calling source to use to resolve some ids that the server generated.

 The following actions have these object responses:

 ##### `add` and `readd`

 This object will be an [Item](../../spec/docs/Item.md), the item that was saved. This allows you to grab the server's item_id and other resolved ids right away.

 ##### `shared_to`

 This object will be a [SharedToResult](../../spec/docs/SharedToResult.md)

 #### `action_errors`

 `action_results` is an array that contains a String with details on why an action failed.

 The first element in `action_results` are the details for the first action in `actions`,
 the second element in `action_results` are the details for the second action in `actions`, and so on.
 If the action didn't fail or doesn't have additional info, it will have a `null` in that position in the array.

 ### `send_guid`

 `https://api.getpocket.com/v3/send_guid`

 Exactly the same as `send` except it can be used while logged out.
 You do not need to send user authentication parameters, just a `guid`.
 It's purpose is for sending analytic actions ahead of log in.
 After login, `send` should be used instead.

 It also only supports a few actions such as {pv}, {pv_wt}, {pv_ab}.
</pre>
 *
 * @see PocketRemoteSource for support of all of Pocket's things and actions.
 */
public class V3Source implements SynchronousSource, FullResultSource, LimitedSource {

	public static JsonConfig JSON_CONFIG = new JsonConfig(PocketRemoteStyle.V3, true);
	public static final String PRODUCTION_SERVER = "https://api.getpocket.com";
	public static final int MAX_ACTIONS_DEFAULT = 30;

	private final EclecticHttp httpClient;
	private final String server;
	private final String parser;
	private Credentials credentials;
	private int maxActions = MAX_ACTIONS_DEFAULT;

	/**
	 * @param server The server that v3 endpoints live on. Pass {@link #PRODUCTION_SERVER} as a default unless you need to override this.
	 * @param parser The article view endpoint, use {@link ArticleView#REMOTE} as a default unless you need to override this.
	 */
	public V3Source(EclecticHttp httpClient, String server, String parser) {
		if (httpClient == null || server == null || parser == null) throw new IllegalArgumentException("missing parameters");
		this.httpClient = httpClient;
		this.server = server;
		this.parser = parser;
		setCredentials(null);
	}

	public synchronized V3Source setCredentials(Credentials credentials) {
		this.credentials = credentials;
		return this;
	}

	/**
	 * See {@link Get#maxActions}
	 * If value is <= 0, this will set it to {@link #MAX_ACTIONS_DEFAULT} instead.
	 */
	public synchronized V3Source setMaxActions(int value) {
		if (value <= 0) value = MAX_ACTIONS_DEFAULT;
		this.maxActions = value;
		return this;
	}
	
	
	/**
	 * Sends the provided actions to v3, handling hitting v3/send or various endpoints as needed.
	 * If all actions are successful, it will also obtain the thing as requested, but only supports
	 * things that have an endpoint ({@link Remote}).
	 */
	@Override
	public synchronized <T extends Thing> T sync(T requested, Action... actions) throws SyncException {
		SyncResult<T> r = syncFull(requested, actions);
		if (r.hasFailures()) {
			throw new SyncException(r);
		} else {
			return r.returned_t;
		}
	}
	
	@Override
	public <T extends Thing> SyncResult<T> syncFull(T requested, Action... actions) {
		if (credentials == null) throw new RuntimeException("missing credentials");
		SyncResult.Builder<T> sr = new SyncResult.Builder<>(requested, actions);
		
		// Try to automatically obtain a GUID if needed
		if (credentials.getGuid() == null) {
			boolean requiresGuid;
			requiresGuid = requested != null && requiresGuid(requested.auth());
			if (!requiresGuid) {
				for (Action a : actions) {
					if (requiresGuid(a.auth())) {
						requiresGuid = true;
						break;
					}
				}
			}
			if (requiresGuid) {
				try {
					Endpoint.Request call = new Endpoint.Request(server + "/v3/" + Guid.REMOTE.address);
					attachCredentials(call, PocketAuthType.NO, null);
					Guid guid = Guid.from(Endpoint.execute(call, httpClient), JSON_CONFIG);
					sr.resolved(guid);
					setCredentials(new Credentials(credentials.getUserAccessToken(), guid.guid, credentials.getDevice(), credentials.getApp()));
				} catch (ApiException e) {
					for (Action action : actions) {
						sr.action(action, Status.FAILED, e, "could not obtain guid");
					}
					if (requested != null) sr.thing(Status.FAILED, e, "could not obtain guid");
					return sr.build();
				}
			}
		}
		
		if (actions.length > 0) {
			List<Action> v3sendActions = new ArrayList<>();
			for (Action a : actions) {
				// Handling depends on the type of action
				if (!isSupported(a)) {
					sr.action(a, Status.IGNORED, null, null);
					
				} else if (a.remote().address != null) {
					try {
						Endpoint.execute(createRequest(a), httpClient);
						sr.action(a, Status.SUCCESS, null, null);
					} catch (Throwable t) {
						ApiException apie = ApiException.unwrap(t);
						if (apie != null) {
							sr.action(a, statusOfRemoteActionFailure(a, apie), t, null);
						} else {
							// Can't be sure if this is a retryable error or not, so default to retryable.
							sr.action(a, Status.FAILED, t, null);
						}
					}

				} else {
					// v3/send based actions will be handled below
					v3sendActions.add(a);
				}
			}
			
			// Handle any v3/send based actions
			if (!v3sendActions.isEmpty()) {
				List<Action> todo = new ArrayList<>(v3sendActions);

				// Decide which send variant we will use based on current credentials
				String endpoint;
				PocketAuthType auth;
				if (credentials.getUserAccessToken() != null) {
					endpoint = "send";
					auth = PocketAuthType.USER;
				} else {
					endpoint = "send_guid";
					auth = PocketAuthType.GUID;

					// If we are using send_guid and there are actions that require login, filter them out.
					for (Action action : v3sendActions) {
						if (action.auth() == PocketAuthType.USER || action.auth() == PocketAuthType.ACCOUNT_MOD) {
							// Not supported by send_guid
							if (action.priority() == RemotePriority.WHENEVER) {
								// These are likely analytics and the analytics team asked that these actions are just ignored and not held to be sent after login, so flag them to be discarded.
								sr.action(action, Status.IGNORED, null, "not logged in, discard");
							} else {
								// Don't attempt, and don't discard since we can process them later if/when they log in.
								sr.action(action, Status.NOT_ATTEMPTED, null, "not logged in, keep for after login");
							}
							todo.remove(action);
						}
					}
				}

				// If any remaining actions aren't supported by the current credentials, don't attempt them.
				for (Action a : new ArrayList<>(todo)) {
					if (!isAuthed(a.auth())) {
						sr.action(a, Status.NOT_ATTEMPTED, null, "missing credentials");
						todo.remove(a);
					}
				}

				// Of any remaining, batch them based on the maxActions setting
				while (!todo.isEmpty()) {
					List<Action> batch = new ArrayList<>(todo.size() <= maxActions ? todo : todo.subList(0, maxActions));
					todo.removeAll(batch);
					try {
						Endpoint.Request call = new Endpoint.Request(server + "/v3/" + endpoint);
						attachCredentials(call, auth, null);
						
						ArrayNode actionsJson = Modeller.OBJECT_MAPPER.createArrayNode();
						for (Action a : batch) {
							actionsJson.add(toV3ActionJson(a, Include.DANGEROUS));
						}
						call.addParam("actions", actionsJson.toString());
						
						ArrayNode results = (ArrayNode) Endpoint.execute(call, httpClient).get("action_results");
						for (int i = 0, len = actionsJson.size(); i < len; i++) {
							JsonNode r = results.get(i);
							Action a = batch.get(i);
							if (r.isBoolean()) {
								if (r.asBoolean()) {
									sr.action(a, Status.SUCCESS, null, null);
								} else {
									sr.action(a, Status.FAILED_DISCARD, null, "v3 returned false");
								}
							} else if (r.isObject()) {
								if (a.action().equals(SharedTo.ACTION_NAME)) {
									sr.resolved(SharedToResult.from(r, JSON_CONFIG));
								} else if (r.has("item_id") && r.has("given_url")) {
									// Assume it is one of the actions like add, readd, etc that have items returned.
									sr.resolved(Item.from(r, JSON_CONFIG));
								} else if (r.has("item")) {
									// V3 used to return item fields "inline" in the action
									// response (like in the if branch above).
									// But V3 Proxy switched to a nested item object.
									sr.resolved(Item.from(r.get("item"), JSON_CONFIG));
								}
								sr.action(a, Status.SUCCESS, null, null);
							} else {
								sr.action(a, Status.FAILED_DISCARD, null, "v3 returned unexpected type " + r.getNodeType());
							}
						}
					} catch (Throwable t) {
						for (Action a : batch) {
							sr.action(a, Status.FAILED, t, null);
						}
					}
				}
			}

			if (sr.hasFailures()) return sr.build();
		}
		
		if (requested != null) {
			if (!isSupported(requested)) {
				sr.thing(Status.IGNORED, null, null);
			} else if (!isAuthed(requested.auth())) {
				sr.thing(Status.FAILED, null, "missing credentials");
			} else {
				try {
					Endpoint.Request call = createRequest(requested);
					T response = (T) Endpoint.execute(call, httpClient, inputStream -> requested.getStreamingCreator().create(Modeller.OBJECT_MAPPER.getFactory().createParser(inputStream), JSON_CONFIG, Allow.UNKNOWN));
					response = (T) response.builder().set(requested.identity()).build();
					response = fixV3Responses(response);
					sr.thing(response);
				} catch (Throwable t) {
					sr.thing(Status.FAILED, t, null);
				}
			}
		}
		return sr.build();
	}

	/**
	 * Builds an {@link com.pocket.sdk.api.endpoint.Endpoint.Request} for a thing or action, determines the right url, attaches all parameters and settings.
	 * @param definition The thing or action
	 * @return A ready to execute request.
	 */
	private Endpoint.Request createRequest(Syncable definition) {
		Remote remote;
		Map<String, Object> map;
		PocketAuthType auth;
		if (definition instanceof Thing) {
			Thing target = ((Thing) definition).identity();
			remote = target.remote();
			auth = (PocketAuthType) target.auth();
			map = target.toMap(Include.DANGEROUS);
		} else {
			Action target = (Action) definition;
			remote = target.remote();
			auth = (PocketAuthType) target.auth();
			map = target.toMap(Include.DANGEROUS);
		}
		Remote.RemoteCallDetails details = Remote.prepare(definition, JSON_CONFIG);
		PocketRemoteSource.cleanupRemoteCallDetails(details);

		String url = details.path;
		switch ((PocketRemoteStyle) remote.style) {
			case PARSER:
				// Override with whatever was supplied in the constructor
				url = parser;
				break;

			case V3:
			default:
				if (!url.startsWith("http://") && !url.startsWith("https://")) {
					url = server + "/v3/" + url;
				}
				break;
		}

		Endpoint.Request call = new Endpoint.Request(url);
		attachCredentials(call, auth, remote.hashTarget);

		Iterator<Map.Entry<String, JsonNode>> jsonit = details.remainingFields.fields(); // Using non-aliased fields so we can lookup the type of field easier
		while (jsonit.hasNext()) {
			Map.Entry<String, JsonNode> e = jsonit.next();
			String fieldname = e.getKey();
			String v3name = remote.toAlias(fieldname, PocketRemoteStyle.V3);

			if (map.get(fieldname) instanceof FileField) {
				FileField file = (FileField) map.get(fieldname);
				call.addFile(v3name, file.asFile());
			}

			JsonNode value = e.getValue();
			if (value != null && !value.isNull()) {
				if (value.isBoolean()) {
					call.addParam(v3name, value.asBoolean() ? 1 : 0);
				} else if (value.isObject() || value.isArray()) { // An example of this is token_info in registerSocialToken and hashes in friendFinderSync
					call.addParam(v3name, value.toString());
				} else {
					call.addParam(v3name, value.asText());
				}
			}
		}

		return call;
	}

	private static boolean requiresGuid(AuthType type) {
		return type == PocketAuthType.GUID ||
				type == PocketAuthType.USER ||
				type == PocketAuthType.USER_OPTIONAL ||
				type == PocketAuthType.LOGIN ||
				type == PocketAuthType.ACCOUNT_MOD;
	}
	
	private void attachCredentials(Endpoint.Request call, PocketAuthType type, String hashTarget) {
		call
			.app(credentials.getApp())
			.device(credentials.getDevice());

		switch (type) {
			case GUID:
			case LOGIN:
				call.guid(credentials.getGuid());
				break;
			case USER:
			case USER_OPTIONAL:
			case ACCOUNT_MOD:
				call.hashTarget(hashTarget);
				call.accessToken(credentials.getUserAccessToken());
				call.guid(credentials.getGuid());
				break;
			case NO:
			default:
				break;
		}
	}

	private boolean isAuthed(AuthType type) {
		boolean hasGuid = credentials.getGuid() != null;
		boolean hasAccessToken = credentials.getUserAccessToken() != null;
		if (type == null) return true;
		switch ((PocketAuthType) type) {
			case GUID:
			case LOGIN:
				return hasGuid;
			case USER_OPTIONAL:
				return hasAccessToken || hasGuid;
			case USER:
			case ACCOUNT_MOD:
				return hasAccessToken;
			case NO:
				return true;
			default:
				return false;
		}
	}
	
	
	/**
	 * Reformat to fit into the Pocket "action" syntax, since {@link Action} has some slight differences.
	 * @param includes See {@link Action#toJson(com.pocket.sync.source.JsonConfig, Include...)}
	 */
	public static ObjectNode toV3ActionJson(Action action, Include... includes) {
		ObjectNode json = action.toJson(JSON_CONFIG, includes);
		JsonNode context = json.remove("context");
		if (context instanceof ObjectNode) {
			json.putAll((ObjectNode) context);
		}
		if (StringUtils2.equalsIgnoreCaseOneOf(action.action(), Add.ACTION_NAME, Readd.ACTION_NAME)) {
			json.remove("item"); // We only use this locally.
		}
		return json;
	}
	
	/**
	 * This remote action threw an {@link ApiException}, decide on what {@link Status} should be returned.
	 */
	private Status statusOfRemoteActionFailure(Action a, ApiException apie) {
		// Specific cases as needed TODO after the backend decides on error handling for this endpoint, perhaps the general handling will cover it, but right now it returns a 500
		if (a instanceof Purchase && StringUtils2.equalsIgnoreCaseOneOf(apie.xErrorCode, "5300", "5301", "5318")) return Status.FAILED_DISCARD;
		
		// General handling
		switch (apie.httpStatusCode) {
			case ApiException.STATUS_DOWN_MAINTENANCE_503:
			case ApiException.STATUS_RATE_LIMIT_403:
			case ApiException.STATUS_USERPASS_401:
				// Didn't fail because of the action specifically
				return Status.FAILED;
				
			case ApiException.STATUS_INVALID_400:
				// Failed because of an invalid action
				return Status.FAILED_DISCARD;
		}
		
		// All other codes, we can't be certain, so default to being retryable
		return Status.FAILED;
	}
	
	
	
	
	/**
	 * Ah the lovely world of workarounds!
	 * A hook for modifying the response from v3 as needed to workaround any quirks of the existing API that doesn't quite fit into the rules of sync.
	 * Ideally any issues would be properly fixed on the v3 side, but this can serve as a last resort for issues that can't be resolved there or would be overly complex to work around in generated code.
	 * @param response The response
	 * @return The same instance or a modified one if changes were required
	 */
	private <T extends Thing> T fixV3Responses(T response) {
		// Parser fixes
		if (response instanceof ArticleView) {
			// Can't trust some values here. See ArticleView.item's docs for more details.
			ArticleView v = (ArticleView) response;
			if (v.item == null) return response;
			
			ObjectNode json = v.item.toJson(JSON_CONFIG, Include.DANGEROUS);
			json.remove("item_id"); // Need to "undeclare" these, otherwise they can erase data we might have already
			json.remove("normal_url");
			Item fixed = Item.from(json, JSON_CONFIG).builder().given_url(v.url).build(); // Ensure the correct url, since the parser can mix these up
			return (T) v.builder().item(fixed).build();
		
		// v3/Get fixes
		} else if (response instanceof Get) {
			// There are some cases in v3 where it doesn't return a field on an item because it really is undeclared
			// but there are some cases where it returns it because it has no value.
			// For example, Item.tags in v3/get. If there are no tags on an item it doesn't return it.
			// We don't have a clear way to decide if the tags are missing because there are no tags or they are missing because v3 didn't return them.
			// At least in the case of v3/get since we pass in parameters that specifically request tags, we can assume if they aren't returned, they really meant to return null.
			// Ideally we ask the v3 implementation to be changed here and return null or empty in this case so we can properly trust declared vs undeclared in all cases.
			// For now, we'll just find these cases and set them as null so they are declared and update locally.
			// Otherwise, if you removed all tags on one device, we wouldn't update on this side.
			Get v = (Get) response;
			if (v.list == null || v.list.isEmpty()) return response;
			List<Item> list = new ArrayList<>(v.list.size());
			boolean tags = Safe.value(v.include_item_tags) == 1;
			boolean positions = Safe.value(v.positions) == 1;
			boolean annotations = Safe.value(v.annotations) == 1;
			boolean posts = Safe.value(v.posts) == 1;
			boolean shares = Safe.value(v.shares) == 1;
			for (Item item : v.list) {
				Item.Builder b = null;
				if (tags && !item.declared.tags) {
					b = b != null ? b : item.builder();
					b.tags(null);
				}
				if (positions && !item.declared.positions) {
					b = b != null ? b : item.builder();
					b.positions(null);
				}
				if (annotations && !item.declared.annotations) {
					b = b != null ? b : item.builder();
					b.annotations(null);
				}
				if (posts && !item.declared.posts) {
					b = b != null ? b : item.builder();
					b.posts(null);
				}
				if (shares && !item.declared.shares) {
					b = b != null ? b : item.builder();
					b.shares(null);
				}
				list.add(b != null ? b.build() : item);
			}
			return (T) v.builder().list(list).build();
		}
		return response;
	}

	@Override
	public boolean isSupported(Syncable syncable) {
		Remote remote = syncable.remote();
		if (remote == null) return false;
		switch ((PocketRemoteStyle) remote.style) {
			case V3:
			case PARSER:
				return true;
			default:
				return false;
		}
	}
}
