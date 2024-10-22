package com.pocket.sync.value.binary;

import org.junit.Assert;
import org.junit.Test;

import okio.Buffer;

/**
 * Tests {@link Varint}
 */
public class VarintTest {
	
	@Test
	public void varint() {
		testInt(Integer.MAX_VALUE);
		testInt(-1);
		testInt(-201);
		testInt(-3002);
		testInt(-40003);
		testInt(-80004);
		testInt(-90005);
		testInt(0);
		testInt(1);
		testInt(201);
		testInt(3002);
		testInt(40003);
		testInt(80004);
		testInt(90005);
		testInt(Integer.MIN_VALUE);
	}
	
	@Test
	public void varlong() {
		testLong(Long.MAX_VALUE);
		testLong(-1);
		testLong(-201);
		testLong(-3000002);
		testLong(-400000003);
		testLong(-8000000004L);
		testLong(-9000000000005L);
		testLong(0);
		testLong(1);
		testLong(201);
		testLong(3000002);
		testLong(400000003);
		testLong(8000000004L);
		testLong(9000000000005L);
		testLong(Long.MIN_VALUE);
	}
	
	private void testInt(int value) {
		Buffer write = new Buffer();
		Varint.writeInt(value, write);
		Buffer read = new Buffer();
		read.write(write.readByteArray());
		Assert.assertEquals(value, Varint.readInt(read));
	}
	
	private void testLong(long value) {
		Buffer write = new Buffer();
		Varint.writeLong(value, write);
		Buffer read = new Buffer();
		read.write(write.readByteArray());
		Assert.assertEquals(value, Varint.readLong(read));
	}
	
}