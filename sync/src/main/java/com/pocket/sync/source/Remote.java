package com.pocket.sync.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pocket.sync.action.Action;
import com.pocket.sync.spec.Syncable;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.value.Include;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Details about how to work with this {@link Thing} or {@link Action} remotely.
 */
public class Remote {

	public enum Method { GET, POST, DELETE }

	/** The style of endpoint. See sync docs for more details. */
	public final RemoteStyle style;
	/** Non-null if requires hitting a specific endpoint rather than the default for the style. Either an absolute url starting with https or a path to append to the default host. */
	public final String address;
	/** The request method. Defaults to GET. */
	public final Method method;
	/** If not null, a value to use in the authentication hash instead of the default target value. (See Pocket's endpoint spec for more details). */
	public final String hashTarget;
	/**
	 * A map of Remote Styles to their aliases
	 * fieldname : remote style : remotename
	 */
	private final Map<String, Map<String, String>> aliasMap;

	/**
	 * @param aliasSets If there are aliases, a list of aliases, repeating (the local name, the remote, then the alias name) for each alias.
	 */
	public Remote(String address, Method method, RemoteStyle style, String hashTarget, String... aliasSets) {
		if (method == null) throw new IllegalArgumentException("method may not be null");
		if (style == null) throw new IllegalArgumentException("style may not be null");
		this.address = address;
		this.method = method;
		this.style = style;
		this.hashTarget = hashTarget;

		aliasMap = aliasSets.length > 0 ? new HashMap<>() : null;

		for (int i = 0, len = aliasSets.length; i < len; i += 3) {
			if (aliasMap.get(aliasSets[i]) == null) {
				aliasMap.put(aliasSets[i], new HashMap<>());
			}
			aliasMap.get(aliasSets[i]).put(aliasSets[i+1], aliasSets[i+2]);
		}
	}

	public String toAlias(String name, RemoteStyle remote) {
		if (remote == null || aliasMap == null) return name;
		Map<String, String> aliases = aliasMap.get(name);
		String alias = null;
		if (aliases != null) {
			alias = aliases.get(remote.toString());
		}
		return alias != null ? alias : name;
	}

	private static Pattern PATH_PARAM = Pattern.compile("\\{\\.([\\w\\d-_]+)\\}");

	/**
	 * Create a {@link RemoteCallDetails} for a thing or action.
	 * @param definition The {@link Thing} or {@link Action} to get details for
	 * @return The {@link RemoteCallDetails} for its address and values.
	 */
	public static RemoteCallDetails prepare(Syncable definition, JsonConfig config) {
		Remote remote = definition.remote();
		String url = remote.address;
		Set<String> inPath = new HashSet<>();
		Matcher matcher = PATH_PARAM.matcher(url);
		ObjectNode json;
		if (definition instanceof Thing) {
			json = ((Thing) definition).identity().toJson(Syncable.NO_ALIASES, Include.DANGEROUS);
		} else {
			json = ((Action) definition).toJson(Syncable.NO_ALIASES, Include.DANGEROUS);
		}
		if (matcher.find()) {
			StringBuffer buffer = new StringBuffer();
			do {
				String field = matcher.group(1);
				boolean declared = json.has(field);
				if (declared) {
					JsonNode value = json.remove(field);
					String replacement = value != null && !value.isNull() ? value.asText() : "null";
					matcher.appendReplacement(buffer, replacement);
					inPath.add(field);
				} else {
					matcher.appendReplacement(buffer, "");
				}
			} while (matcher.find());
			url = buffer.toString();
		}
		ObjectNode aliased;
		if (remote.aliasMap == null) {
			aliased = json;
		} else {
			aliased = json.deepCopy();
			for (String local : remote.aliasMap.keySet()) {
				if (inPath.contains(local)) continue;
				aliased.set(remote.aliasMap.get(local).get(config.getRemote().toString()), aliased.remove(local));
			}
		}
		return new RemoteCallDetails(definition, url, inPath, json, aliased);
	}
	/** A bunch of helpful info for working with a specific instance of a thing or action remotely. Use {@link #prepare(Syncable, JsonConfig)} to create. */
	public static class RemoteCallDetails {
		/** The thing/action this was used. */
		public final Object thingOrAction;
		/** The {@link Remote#address} value with any path replacements filled in. */
		public final String path;
		/** The names of fields that were put into the path. (This can be used to know what fields still need to be included via a query or other means) */
		public final Set<String> pathFields;
		/** The json of the action/thing, only including fields that weren't already included in the path. */
		public final ObjectNode remainingFields;
		/** The same as {@link #remainingFields} but the keys switched to their aliases as needed */
		public final ObjectNode remainingFieldsAliased;
		private RemoteCallDetails(Object thingOrAction, String path, Set<String> pathFields, ObjectNode remainingFields, ObjectNode remainingFieldsAliased) {
			this.thingOrAction = thingOrAction;
			this.path = path;
			this.pathFields = pathFields;
			this.remainingFields = remainingFields;
			this.remainingFieldsAliased = remainingFieldsAliased;
		}
	}
}
