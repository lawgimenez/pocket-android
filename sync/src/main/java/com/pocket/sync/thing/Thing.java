package com.pocket.sync.thing;

import com.pocket.sync.source.subscribe.Changes;
import com.pocket.sync.space.Diff;
import com.pocket.sync.space.Space;
import com.pocket.sync.space.mutable.MutableThing;
import com.pocket.sync.space.mutable.Mutables;
import com.pocket.sync.spec.Reactions;
import com.pocket.sync.spec.Syncable;
import com.pocket.sync.value.ByteTypeParser;
import com.pocket.sync.value.Include;
import com.pocket.sync.value.StreamingThingParser;
import com.pocket.sync.value.SyncableParser;
import com.pocket.sync.value.binary.ByteReader;
import com.pocket.sync.value.binary.ByteWriter;
import com.pocket.sync.value.protect.StringEncrypter;

/**
 * The state and details of a Thing.
 * Part of the Sync framework, see {@link com.pocket.sync} for more details.
 */
public interface Thing extends Syncable {

	/** If a thing needs to make sure its type is included in serialized formats, this is the expected key. */
	String SERIALIZATION_TYPE_KEY = "_type";
	
	/**
	 * A unique name for this type of {@link Thing}. There can be many different ones of the same type.
	 * For example, a "cat", or "animal". Similar to a class name.
	 */
	String type();
	
	/**
	 * @return A copy of this {@link Thing} with only its identifying values.
	 * If the thing is not identifiable it just returns its self.
	 */
	Thing identity();
	
	/**
	 * @return true if this type of {@link Thing} is identifiable. This value should always be the same for this type, regardless of the instances missing identity values.
	 */
	boolean isIdentifiable();
	
	/**
	 * A unique hash string that represents this thing's identity.
	 * Things that are the same type and have the same identity will have the same value.
	 * This can be more reliable than {@link #hashCode()} in terms of potential collisions.
	 * This is up to the implementation, but one way to do this is to:
	 * <ol>
	 *     <li>Create a {@link ByteWriter}</li>
	 *     <li>{@link ByteWriter#writeString(String)} with the {@link #type()}</li>
	 *     <li>{@link ByteWriter#writeString(String)} with the {@link #identity()} {@link #toJson(Include...)} (with dangerous fields, and fields sorted in alphabetical order), and {@link #toString()}</li>
	 *     <li>Produce a SHA-256 hash via {@link ByteWriter#sha256()} This has is the idkey</li>
	 * </ol>
	 */
	String idkey();
	
	SyncableParser getCreator();
	StreamingThingParser getStreamingCreator();
	ByteTypeParser getByteCreator();
	
	enum Equality {
		/** Only compare identity fields. If it is not {@link #isIdentifiable()},  it falls back to comparing {@link #STATE}. */
		IDENTITY,
		/** Compare all fields. Be aware that fields default to null so a Thing that knows a field is null and a Thing that doesn't know about that field will be considered equal because both fields are null. */
		STATE,
		/** Compare only fields that are declared in both things. If one or both things don't have a field declared, it is ignored. */
		STATE_DECLARED,
		/** Compare only fields that are "owned" by the object. Like {@link #STATE} but when comparing a field value that is an identifiable thing, it only compares their identity. */
		FLAT,
		/** Same as {@link #FLAT} but for fields that are collections and maps, it only compares null and size. Does not compare contents. */
		FLAT_SIZED
	}
	
	boolean equals(Equality e, Object o);
	
	/**
	 * A {@link #hashCode()} implementation that will follow a {@link Equality} rule.
	 * By default {@link Thing#hashCode()} will use {@link Equality#IDENTITY}
	 * Currently only two rules are supported: {@link Equality#IDENTITY} and {@link Equality#STATE}.
	 * The results of other equality rules are undetermined.
	 */
	int hashCode(Equality e);
	
	/**
	 * Adds all identifiable things contained within this thing to the {@link FlatUtils.Output}.
	 * See {@link FlatUtils} for utility methods rather than using this method directly.
	 */
	void subthings(FlatUtils.Output out);
	
	/**
	 * @return An instance of this thing where all of the identifiable things it references are reduced to {@link #identity()}. If it has no such references it will return the existing instance.
	 */
	Thing flat();
	
	/**
	 * If the child (matching by the provided criteria) is contained within the shallow part of this Thing (meaning it won't search within child identifiable things)
	 * it will return a new instance with the provided child state replacing the current one.
	 * It will do this for the first match it finds. If there are two instances of the exact same child, it will only replace the first one.
	 * TODO support multiple instances at some point?
	 * If no changes, it should return null to indicate nothing changed
	 */
	Thing with(Changes.ThingMatch match, Thing replace);

	/** @return A new builder of this Thing's type with the declared values of this Thing already set. Note: Each thing instance is immutable so changes to the builder won't have any impact on this instance. Instead the builder will create a new immutable instance when you invoke {@link ThingBuilder#build()}. */
	ThingBuilder builder();
	
	/**
	 * @param root See {@link MutableThing#root()} for more details
	 * @return A {@link MutableThing} with the same state.
	 */
	MutableThing mutable(Mutables mutables, MutableThing root);
	
	/**
	 * Compares the state of what the thing "was" to what it "is" now and looks for changes that might cause reactions in another thing.
	 * <p>
	 * As an API this is a bit backwards, normally a class wouldn't contain the knowledge about what another class should react to,
	 * however flipping this logic does have a lot of performance benefits. This heavily relies on code generation
	 * being able to seed this method with knowledge ahead of time.
	 * <p>
	 * As an example, let's say we have the following two things defined:
	 * <pre>
	 *     thing A {
	 *         field : String
	 *     }
	 *     thing B {
	 *         field : String {
	 *             (A.field)
	 *             ~ Derive something
	 *         }
	 *     }
	 * </pre>
	 *
	 * Using these definitions, we know ahead of time, at code generation time, that if A.field ever changes, then B.field likely needs to rederive.
	 * So if we invoke a.reactions(was, is, r) this A's implementation would be to look at its `field` and if it changed, notify that B.field likely
	 * needs to be rederived.
	 * <p>
	 * While this does seem backwards, it is a more direct/faster method for {@link Space#imprint} to determine what reactions might need to happen.
	 * <p>
	 * For implementations: The general pattern for comparing if a field changed enough to react is:
	 * <pre>
	 * if (is.declared.field && (was == null || !was.declared.field || ObjectUtils.notEqual(was.field, is.field))) {
	 *     // changed for reactive
	 * }
	 * </pre>
	 * In addition to looking for changes to reactive triggers, if there is a derived field that is undeclared, it should also output it as a reaction to notify that it needs to be derived.
	 * <pre>
	 * if (!is.declared.field) {
	 *     // derive for first time or rederive because changed
	 * }
	 * </pre>
	 * Implementations can assume that this thing has changed, it just needs to look at what fields changed.
	 * TODO use generics to force was and is to the same type, for now implementations can assume and cast.
	 */
	void reactions(Thing was, Thing is, Diff diff, Reactions reactions);
	
	/**
	 * If this thing contains any dangerous fields, return a new instance with those fields redacted.
	 * If it doesn't have any fields like this, it may just return the existing instance.
	 * Redaction is up the the internal implementations to decide how to handle.
	 * @param encrypter A {@link StringEncrypter} that can be used if any implementations wants to encrypt values.
	 */
	Thing redact(StringEncrypter encrypter);
	
	/**
	 * Returns a new instance that undoes {@link #redact(StringEncrypter)}, or possibility the same instance if nothing needed to change.
	 */
	Thing unredact(StringEncrypter encrypter);
	
	/**
	 * Write out this thing as bytes in a way that can be later restored.
	 * See {@link #getByteCreator()}, {@link com.pocket.sync.spec.Spec.Things#thing(String, ByteReader)}, or a generated thing's from(ByteReader) method for uncompressing.
	 * See the "Compression" section of the sync docs for more info.
	 * <p>
	 * <b>WARNING</b> This will always include {@link Include#DANGEROUS} values.
	 * This doesn't implement the {@link Include} filtering to avoid the overhead and complexity of determining what mode the byte[] might be in and whether or not to expect those fields.
	 * Even though it will be raw binary data, values could still be reconstructed, so if you are going to store this information or expose it in some unprotected way, use {@link #redact(StringEncrypter)} first.
	 */
	void compress(ByteWriter out);

	/**
	 * Some APIs can return either arrays of values or Things directly as the outermost Json.
	 * Root value is the name given (in the schema) to the value returned by the endpoint, allowing these
	 * endpoints to operate as regular things. This value is client side denoted and arbitrary.
	 *
	 * @return the name for the root value of this Thing, or null if it does not have one.
	 */
	String rootValue();
}
