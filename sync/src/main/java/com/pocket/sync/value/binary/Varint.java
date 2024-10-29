// This implementation is based on "Varint" related code in Google's Protocol Buffers Library.
// Their open source license is as follows:
//
// Protocol Buffers - Google's data interchange format
// Copyright 2008 Google Inc.  All rights reserved.
// https://developers.google.com/protocol-buffers/
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//     * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//     * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//     * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


package com.pocket.sync.value.binary;

import java.io.EOFException;

import okio.Buffer;

/**
 * Helper methods for packing numbers into the minimal amount of bytes based on size.
 *
 * This is based on the concept and code in Google's Protocol Buffer library.
 * In some places it follows their logic exactly, in others we do things a bit differently.
 * See docs below for more details.
 *
 * Based on concepts described here: https://developers.google.com/protocol-buffers/docs/encoding#varints
 * and code found here https://github.com/protocolbuffers/protobuf/blob/master/java/core/src/main/java/com/google/protobuf/CodedOutputStream.java
 * and here https://github.com/protocolbuffers/protobuf/blob/master/java/core/src/main/java/com/google/protobuf/CodedInputStream.java
 *
 * Also helpful : https://developers.google.com/protocol-buffers/docs/proto3#scalar
 *
 * In this class we'll use the following terms in translation to theirs:
 *
 * <pre>
 * We call it       Proto calls it      Notes
 * int          ->  int32               They say negative numbers used here are supported but slower. We don't have a lot of negative numbers in our use cases, so we won't worry about those optimizations
 * long         ->  int64               Ditto about negatives
 * </pre>
 *
 */
public class Varint {
	
	/**
	 * Based on writeInt32NoTag()
	 */
	public static void writeInt(int value, Buffer buffer) {
		if (value >= 0) {
			writeUInt32NoTag(value, buffer);
		} else {
			// Must sign-extend.
			writeLong(value, buffer);
		}
	}
	private static void writeUInt32NoTag(int value, Buffer buffer) {
		while (true) {
			if ((value & ~0x7F) == 0) { 		// If the first bit is 0   (0x7F is 0111111 and ~0x7F is 10000000) & only keeps bits that both ~0x7F and value have, so will only be !=- 0 if value has a 1 in the first bit
				buffer.writeByte((byte) value); // write the value, due to the loop, this should be within byte's maximum value
				return;
			} else { 							// First bit is 1
				buffer.writeByte((byte) ((value & 0x7F) | 0x80));   // 0x7F is 0111111,  0x80 is 10000000   (value & 0x7F) keeps only the 7 bits on the right, then | 0x80 sets the left most bit to 1
				value >>>= 7; 					// Trims off the 7 bytes on the right we just wrote
			}
		}
	}
	
	/**
	 * Based on writeUInt64NoTag()
	 */
	public static void writeLong(long value, Buffer buffer) {
		while (true) {
			if ((value & ~0x7F) == 0) {
				buffer.writeByte((byte) value);
				return;
			} else {
				buffer.writeByte((byte) (((int) value & 0x7F) | 0x80));
				value >>>= 7;
			}
		}
	}
	
	/**
	 * Based on readRawVarint32() from https://github.com/protocolbuffers/protobuf/blob/master/java/core/src/main/java/com/google/protobuf/CodedInputStream.java#L3566
	 */
	public static int readInt(Buffer buffer) {
		int x;
		try {
			if ((x = buffer.readByte()) >= 0) {
				return x;
			} else if ((x ^= (buffer.readByte() << 7)) < 0) {
				x ^= (~0 << 7);
			} else if ((x ^= (buffer.readByte() << 14)) >= 0) {
				x ^= (~0 << 7) ^ (~0 << 14);
			} else if ((x ^= (buffer.readByte() << 21)) < 0) {
				x ^= (~0 << 7) ^ (~0 << 14) ^ (~0 << 21);
			} else {
				int y = buffer.readByte();
				x ^= y << 28;
				x ^= (~0 << 7) ^ (~0 << 14) ^ (~0 << 21) ^ (~0 << 28);
				if (y < 0
						&& buffer.readByte() < 0
						&& buffer.readByte() < 0
						&& buffer.readByte() < 0
						&& buffer.readByte() < 0
						&& buffer.readByte() < 0) {
					throw new RuntimeException();
				}
			}
		} catch (EOFException eof) {
			throw new IllegalStateException(eof);
		}
		return x;
	}
	
	/**
	 * Based on readRawVarint64() https://github.com/protocolbuffers/protobuf/blob/master/java/core/src/main/java/com/google/protobuf/CodedInputStream.java#L1050
	 */
	public static long readLong(Buffer buffer) {
		long x;
		int y;
		try {
			if ((y = buffer.readByte()) >= 0) {
				return y;
			} else if ((y ^= (buffer.readByte() << 7)) < 0) {
				x = y ^ (~0 << 7);
			} else if ((y ^= (buffer.readByte() << 14)) >= 0) {
				x = y ^ ((~0 << 7) ^ (~0 << 14));
			} else if ((y ^= (buffer.readByte() << 21)) < 0) {
				x = y ^ ((~0 << 7) ^ (~0 << 14) ^ (~0 << 21));
			} else if ((x = y ^ ((long) buffer.readByte() << 28)) >= 0L) {
				x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28);
			} else if ((x ^= ((long) buffer.readByte() << 35)) < 0L) {
				x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35);
			} else if ((x ^= ((long) buffer.readByte() << 42)) >= 0L) {
				x ^= (~0L << 7) ^ (~0L << 14) ^ (~0L << 21) ^ (~0L << 28) ^ (~0L << 35) ^ (~0L << 42);
			} else if ((x ^= ((long) buffer.readByte() << 49)) < 0L) {
				x ^=
						(~0L << 7)
								^ (~0L << 14)
								^ (~0L << 21)
								^ (~0L << 28)
								^ (~0L << 35)
								^ (~0L << 42)
								^ (~0L << 49);
			} else {
				x ^= ((long) buffer.readByte() << 56);
				x ^=
						(~0L << 7)
								^ (~0L << 14)
								^ (~0L << 21)
								^ (~0L << 28)
								^ (~0L << 35)
								^ (~0L << 42)
								^ (~0L << 49)
								^ (~0L << 56);
				if (x < 0L) {
					if (buffer.readByte() < 0L) throw new RuntimeException();
				}
			}
		} catch (EOFException eof) {
			throw new IllegalStateException(eof);
		}
		return x;
	}
	
	
}
