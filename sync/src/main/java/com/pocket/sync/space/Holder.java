package com.pocket.sync.space;

import com.pocket.sync.thing.Thing;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;


/**
 * Represents some entity that wants to keep one or many {@link Thing}s available in {@link Space}.
 * See {@link Space#remember(Holder, Thing...)} and {@link Space#forget(Holder, Thing...)}.
 * <p>
 * Holders are uniquely identified by their {@link #key()}. Creating an instance with the same key always refers to the same holder,
 * this allows you to refer to the same holder each time the app starts up. You don't need to use the same instance of {@link Holder}
 * as long as you used the same key, {@link Space} will consider any instance with the same key, the same holder.
 * <p>
 * Holders also specify the type of {@link Hold} they want. All holds are always held from {@link Space#remember(Holder, Thing...)} and released with {@link Space#forget(Holder, Thing...)},
 * but {@link Hold} can request some additional kinds of behaviour or handling. Users of this class should always pair the same holder key with hold type, using the same key with multiple
 * types has an unspecified behaviour, nothing enforces this, so it is up the user to ensure these are uniquely paired.
 * <p>
 * Create instances with the static methods like {@link #persistent(String)} and {@link #session(String)}.
 */
public class Holder {
	
	/** Creates a new holder as {@link Hold#PERSISTENT} */
	public static Holder persistent(String name) {
		return new Holder(name, Hold.PERSISTENT);
	}
	
	/** Creates a new holder as {@link Hold#SESSION} */
	public static Holder session(String name) {
		return new Holder(name, Hold.SESSION);
	}
	
	public static Holder from(String holdKey, String name) {
		Hold type = null;
		for (Hold hold : Hold.values()) {
			if (holdKey.equals(hold.key)) {
				type = hold;
				break;
			}
		}
		if (type == null || name == null) return null;
		return new Holder(name, type);
	}
	
	/**
	 * All holds are always held from {@link Space#remember(Holder, Thing...)} to {@link Space#forget(Holder, Thing...)},
	 * but these different types of holds can specify additional requested behaviour.
	 */
	public enum Hold {
		/**
		 * Performs the basic contract of being held until forget is called.
		 * <p>
		 * Use when you want something to be available offline, persist across app instances, reboots, etc.
		 * You must becareful to manage this data as it will stay around until you explicitly forget it.
		 * <p>
		 * <b>This can be dangerous</b> because it means if you don't ever call forget with this holder
		 * it will be in space permanently. Users of this need to be very careful to manage this correctly.
		 * A case to be mindful of is when a feature requests this and you later remove the feature from the code
		 * base. Even though the feature code is gone, you should still make sure you release its old holds.
		 */
		PERSISTENT("persist"),
		
		/**
		 * Held for the rest of the session or until forget is called. Use for temporary holds, subscriptions, etc.
		 * <p>
		 * "Session" is up to the implementation to determine but likely related to as long as the app process is active.
		 * Implementations use {@link Space#forgetSession()} to release a session. A common case would be invoking it
		 * on app boot to clear out anything from the previous session that wasn't removed explicitly.
		 * <p>
		 * This is a very safe hold, because if you never call forget, it will still be cleaned up sometime
		 * in the future.
		 * <p>
		 * This is also considered to be active and implementations may use this as an indication
		 * to keep data in a way that can be more quickly accessed and changed. This is up
		 * to the implementation, but this may lead to better performance on things held this way.
		 */
		SESSION("session");
		
		// Some other holds that could maybe be useful in the future:
		// FAST_BOOT A hold that hints to the space implementation that this thing is important to be able to cold load quickly, could be useful for ui that we want to avoid spinners. The space implementation could try to persist it in a way that it can read it from cold storage really quickly. (turbo mode)
		// LONG_TERM A hold that times out, so a slightly safer variant of PERSISTENT, that could release after some timeout from being accessed the last time, so if the app doesn't access it for a few days it could release it.
		
		
		/** A unique id for this hold type that will NEVER CHANGE in app updates and is suitable for persisting. */
		public final String key;
		Hold(String key) {
			this.key = key;
		}
	}
	
	private final String key;
	private final Hold hold;
	
	private Holder(String key, Hold hold) {
		if (StringUtils.isBlank(key)) {
			throw new IllegalArgumentException("holder key may not be blank");
		} else if (hold == null) {
			throw new IllegalArgumentException("holder hold may not be null");
		}
		this.key = key;
		this.hold = hold;
	}
	
	public String key() {
		return key;
	}
	
	public Hold hold() {
		return hold;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Holder holder = (Holder) o;
		return key.equals(holder.key);
	}
	
	@Override
	public int hashCode() {
		return key.hashCode();
	}
	
	@NotNull
	@Override
	public String toString() {
		return "Holder:"+key();
	}
}
