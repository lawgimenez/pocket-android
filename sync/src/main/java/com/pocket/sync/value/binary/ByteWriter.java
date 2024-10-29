package com.pocket.sync.value.binary;

import okio.Buffer;
import okio.Utf8;

/**
 * A sink for writing an unknown number of bytes, in various value types and then retrieving the resulting byte[].
 * See {@link ByteReader} for reversing this.
 * See {@link com.pocket.sync.thing.Thing#compress(ByteWriter)} for more details.
 */
public class ByteWriter {
	
	private final Buffer buffer = new Buffer();
	
	/** Flags to use to flip a specific bit within a byte. */
	private final int[] bit = new int[] {
		0x1,   // 0000 0001   index 0
		0x2,   // 0000 0010   index 1
		0x4,   // 0000 0100   index 2
		0x8,   // 0000 1000   index 3
		0x10,  // 0001 0000   index 4
		0x20,  // 0010 0000   index 5
		0x40,  // 0100 0000   index 6
		~0x7F  // 1000 0000   index 7
	};
	/** When using {@link #writeBit(boolean)}, what bit will be written next. */
	private int bitIndex = 0;
	/** When using {@link #writeBit(boolean)}, the current byte value that is being constructed and hasn't been written to the buffer yet.. */
	private byte _byte = 0;
	
	/**
	 * Write the next bit.
	 * This does not write it to the buffer until this has been invoked 8 times (completed a full byte) or when calling {@link #finishByte()}.
	 * @param value true to write a 1, false to write a 0.
	 * @return The value you provided.
	 */
	public boolean writeBit(boolean value) {
		if (value) _byte |= bit[bitIndex]; // defaulted to 0, so only need to flip if writing a 1
		bitIndex++;
		if (bitIndex == 8) finishByte();
		return value;
	}
	
	/**
	 * If there are bits from via {@link #writeBit(boolean)} that haven't been written to the buffer yet, write the byte to the buffer.
	 * Any bits not set are 0.
	 * After calling this, the next {@link #writeBit(boolean)} call starts at the first bit again.
	 */
	public void finishByte() {
		if (bitIndex == 0) return;
		buffer.writeByte(_byte);
		_byte = 0;
		bitIndex = 0;
	}
	
	public void writeString(String value) {
		writeLong(Utf8.size(value));
		buffer.writeUtf8(value);
	}
	
	public void writeInt(int value) {
		Varint.writeInt(value, buffer);
	}
	
	public void writeLong(long value) {
		Varint.writeLong(value, buffer);
	}
	
	public void writeDouble(double value) {
		buffer.writeDecimalLong(Double.doubleToLongBits(value));
	}
	
	public void writeBoolean(boolean value) {
		buffer.writeByte(value ? 1 : 0);
	}
	
	/**
	 * Returns the bytes that have been written so far.
	 * If there are flushed bits from {@link #writeBit(boolean)}, they are discarded.
	 * After invoking this, this buffer will be empty and reset back to its original state.
	 */
	public byte[] readByteArray() {
		bitIndex = 0;
		_byte = 0;
		return buffer.readByteArray();
	}
	
	/**
	 * @return A SHA-256 hash hex string of the current bytes.
	 */
	public String sha256() {
		return buffer.sha256().hex();
	}
	
}
