package com.pocket.sdk.api.raw;

import com.pocket.sdk.api.value.HexColor;

import org.junit.Assert;
import org.junit.Test;

public class HexColorTest {
	
	@Test
	public void throwsIfEmpty() {
		try {
			new HexColor(null);
			Assert.fail("did not throw exception");
		} catch (Throwable ignored) {}
		try {
			new HexColor("");
			Assert.fail("did not throw exception");
		} catch (Throwable ignored) {}
	}
	
	@Test
	public void throwsIfWrongSize() {
		try {
			new HexColor("#123");
			Assert.fail("did not throw exception");
		} catch (Throwable ignored) {}
		try {
			new HexColor("#1232312313");
			Assert.fail("did not throw exception");
		} catch (Throwable ignored) {}
	}
	
	@Test
	public void throwsIfNotHex() {
		try {
			new HexColor("not hex");
			Assert.fail("did not throw exception");
		} catch (Throwable ignored) {}
		try {
			new HexColor("#nothex");
			Assert.fail("did not throw exception");
		} catch (Throwable ignored) {}
		try {
			new HexColor("#ff00T0");
			Assert.fail("did not throw exception");
		} catch (Throwable ignored) {}
	}
	
	@Test
	public void validates() {
		new HexColor("#ff00ff");
		new HexColor("#ff00ffaa");
	}
	
}