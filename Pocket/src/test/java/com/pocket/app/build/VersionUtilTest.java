package com.pocket.app.build;

import com.pocket.app.VersionUtil;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link VersionUtil}
 */
public class VersionUtilTest {
	
	@Test
	public void toVersionCode() {
		Assert.assertEquals(VersionUtil.toVersionCode(0,0,0,0), 0);
		Assert.assertEquals(VersionUtil.toVersionCode(0,0,0,1), 1);
		Assert.assertEquals(VersionUtil.toVersionCode(7,0,38,384), 70038384);
		Assert.assertEquals(VersionUtil.toVersionCode(7,0,3,384), 70003384);
		Assert.assertEquals(VersionUtil.toVersionCode(107, 0, 3, 384), 1070003384);
		Assert.assertEquals(VersionUtil.toVersionCode(4, 0), 40000000);
	}
	
	@Test
	public void toVersionName() {
		Assert.assertEquals(VersionUtil.toVersionName(0), "0.0.0.0");
		Assert.assertEquals(VersionUtil.toVersionName(1), "0.0.0.1");
		Assert.assertEquals(VersionUtil.toVersionName(70038384), "7.0.38.384");
		Assert.assertEquals(VersionUtil.toVersionName(70003384), "7.0.3.384");
		Assert.assertEquals(VersionUtil.toVersionName(1070003384), "107.0.3.384");
		Assert.assertEquals(VersionUtil.toVersionName(40000000), "4.0.0.0");
	}
	
}