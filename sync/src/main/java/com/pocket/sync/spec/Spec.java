package com.pocket.sync.spec;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pocket.sync.action.Action;
import com.pocket.sync.source.JsonConfig;
import com.pocket.sync.source.SynchronousRemoteBackedSource;
import com.pocket.sync.source.result.RemotePriority;
import com.pocket.sync.space.Diff;
import com.pocket.sync.space.Space;
import com.pocket.sync.thing.Thing;
import com.pocket.sync.value.binary.ByteReader;

import java.io.IOException;
import java.util.Collection;

/**
 * The domain logic of your API.
 * <p>
 * Declares what {@link Thing}s and {@link Action}s are available to play with,
 * and provides the logic for how {@link Action}s effect {@link Thing}s and {@link Space}.
 */

public interface Spec {
	
	Things things();
	Actions actions();
	Derive derive();
	
	interface Things {
		Thing thing(String type, ObjectNode json, JsonConfig config);
		Thing thing(String type, JsonParser parser, JsonConfig config) throws IOException;
		Thing thing(String type, ByteReader bytes);
	}
	
	interface Actions {
		Action action(ObjectNode json, JsonConfig config);
	}
	
	interface Derive {
		
		/**
		 * Attempt to derive/create the provided thing out of other known things in space.
		 * If it is possible, return a new instance with only the fields you knew how to derive declared.
		 * If it isn't possible to derive it, or if you don't know how to derive it, return null.
		 */
		<T extends Thing> T derive(T thing, Space.Selector space);
		
		/**
		 * Update any derived fields within this {@link Thing} and return an updated instance or the same if no changes are needed.
		 * Only "flat" values that it "owns" in this thing should be modified.
		 * <p>
		 * One case of modifying "other" state that is allowed is to create a new reference to another identifiable thing.
		 * For example, if this thing references another thing, this may change or create a reference to another thing.
		 * By doing that, it might end up referencing a thing that doesn't exist in the space, so that could end up "creating" this thing.
		 * That is a subtle "other" state modification that is allowed.
		 * But note, that any state within that reference may be ignored, the only change allowed here is the reference itself.
		 *
		 * @param thing The latest state of the thing
		 * @param reactions Null to rederive all derived fields, or list of specific field names to rederive.
		 * @param diff If due to a {@link Space#imprint(Thing)}, a record of everything that changed in the imprint.
		 * @param selector A selector for querying things needed to derive its new value, only needed if there are derived fields that depend on other things. If null, and there are fields like that, they won't be rederived.
		 * @return An updated instance or the same if no changes were made
		 */
		<T extends Thing> T rederive(T thing, Collection<String> reactions, Diff diff, Space.Selector selector);
	}
	
	/**
	 * Make the {@link Space} reflect changes from this action.
	 * <p>
	 * If the change cannot be done locally and requires interaction with the connected source, you can use the provided source to access it.
	 * Any things synced through that source will be imprinted locally. Avoid this if possible, it is best if actions can be applied locally
	 * as it won't block others on calls to the connected source.
	 * <p>
	 * NOTE: For sources and specs that are not {@link SynchronousRemoteBackedSource} the `remote` parameter
	 * will always be null and the returned {@link RemotePriority} will be ignored.
	 * (Author Note: For now I opt'd to keep this one method rather than making a separate one for local only and another for remote/linked
	 * to keep the implementations simpler and reuse more code.  I could see the value of making them separate interfaces, but opt'd for this for now.)
	 *
	 * @return When the action should be sent to the connected source. This is only a request/suggestion.  If you already sent it to the connected source as part of this, return {@link RemotePriority#HANDLED}.
	 * 		For Pocket, Most stateful actions that were applied locally will likely use {@link RemotePriority#SOON}.
	 * 					Analytics like actions will typically use {@link RemotePriority#WHENEVER}.
	 */
	void apply(Action action, Space space);
	
}
