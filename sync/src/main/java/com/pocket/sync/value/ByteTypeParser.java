package com.pocket.sync.value;

import com.pocket.sync.value.binary.ByteReader;

/**
 * Interface for creating values from the next byte(s) in a {@link ByteReader}
 */
public interface ByteTypeParser<T> {
	/** Read this value type out of the provided reader and return it. */
	T create(ByteReader value);
}
