package com.pocket.sync.value;

import com.pocket.sync.value.binary.ByteWriter;

import org.jetbrains.annotations.NotNull;


/**
 * Base class for generated enums
 * @param <V> The type of value. This type must support equals, hashcode and toString
 */
public class EnumType<V> {
	
	public final V value;
	/** An id that represents this enum value consistently across versions of the app. Used when compressing/uncompressing this value during {@link com.pocket.sync.thing.Thing#compress(ByteWriter)}. */
	public final int id;
	/** A String representation of this Enum.  For IntegerEnums this will be the String alias defined in figment.  Otherwise, this will be a String of the value. */
	public final String name;
	
	protected EnumType(V value, int id, String name) {
		if (value == null) throw new IllegalArgumentException("value cannot be null");
		this.value = value;
		this.id = id;
		this.name = name;
	}
	
	public V getValue() {
		return value;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		EnumType<?> enumType = (EnumType<?>) o;
		return value.equals(enumType.value);
	}
	
	@Override
	public int hashCode() {
		return value.hashCode();
	}
	
	@NotNull
	@Override
	public String toString() {
		return value.toString();
	}

}
