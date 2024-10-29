package com.pocket.sync.value.protect;

import org.apache.commons.lang3.StringUtils;

/**
 * An implementation that makes a simple change for the purposes of testing.
 * This is not secure and not intended to be used for real world code.
 */
public class OnlyForTestingEncrypter implements StringEncrypter {
	@Override
	public String encrypt(String value) {
		return StringUtils.reverse(value);
	}
	
	@Override
	public String decrypt(String value) {
		return StringUtils.reverse(value);
	}
}
