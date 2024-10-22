package com.pocket.sync.value.binary;

import com.pocket.sync.value.ByteTypeParser;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okio.Buffer;

/**
 * A parser of a byte[] (that was constructed by {@link ByteWriter} to concrete value types.
 * See {@link com.pocket.sync.thing.Thing#compress(ByteWriter)} for more details.
 * <p>
 * Each read call, reads the next bytes as that value type and consumes those bytes, removing them from this buffer.
 * <p>
 * Can be reused after consuming all of its bytes.
 */
public class ByteReader {
	
	private final Buffer buffer = new Buffer();
	
	private int bitIndex = 8;
	private byte _byte;
	
	/**
	 * New instance with a byte[] instead of invoking {@link #load(byte[])}
	 */
	public ByteReader(byte[] data) {
		buffer.write(data);
	}
	
	/**
	 * New instance with an empty buffer. Use {@link #load(byte[])} to fill it.
	 */
	public ByteReader() {}
	
	/**
	 * Add these bytes to the buffer.
	 */
	public void load(byte[] data) {
		buffer.write(data);
	}
	
	/**
	 * Reads the next bit.
	 * When you are done reading bits, invoke {@link #finishByte()}.
	 */
	public boolean readBit() {
		if (bitIndex == 8) {
			try {
				_byte = buffer.readByte();
			} catch (EOFException eof) {
				throw new IllegalStateException(eof);
			}
			bitIndex = 0;
		}
		return ((_byte >> bitIndex++) & 1) == 1;
	}
	
	/**
	 * Stop reading the current in progress byte.
	 * The next {@link #readBit()} will pull out a new byte to start reading from the start.
	 */
	public void finishByte() {
		bitIndex = 8;
		_byte = 0;
	}
	
	public <V> List<V> readList(ByteTypeParser<V> creator, boolean nullableElements) {
		int size = readInt();
		List<V> list = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			if (nullableElements) {
				if (readBoolean()) {
					list.add(creator.create(this));
				} else {
					list.add(null);
				}
			} else {
				list.add(creator.create(this));
			}
		}
		return list;
	}
	
	public <V> Map<String, V> readMap(ByteTypeParser<V> creator, boolean nullableElements) {
		int size = readInt();
		HashMap<String, V> map = new HashMap<>(size);
		for (int i = 0; i < size; i++) {
			String key = readString();
			if (nullableElements) {
				if (readBoolean()) {
					map.put(key, creator.create(this));
				} else {
					map.put(key, null);
				}
			} else {
				map.put(key, creator.create(this));
			}
		}
		return map;
	}
	
	public String readString() {
		try {
			return buffer.readUtf8(readLong());
		} catch (EOFException e) {
			throw new RuntimeException(e);
		}
	}
	
	public int readInt() {
		return Varint.readInt(buffer);
	}
	
	public long readLong() {
		return Varint.readLong(buffer);
	}
	
	public double readDouble() {
		try {
			return Double.longBitsToDouble(buffer.readDecimalLong());
		} catch (EOFException eof) {
			throw new IllegalStateException(eof);
		}
	}
	
	public boolean readBoolean() {
		try {
			return buffer.readByte() == 1;
		} catch (EOFException eof) {
			throw new IllegalStateException(eof);
		}
	}
	
}
