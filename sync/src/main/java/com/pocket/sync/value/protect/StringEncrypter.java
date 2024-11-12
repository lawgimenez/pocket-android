package com.pocket.sync.value.protect;

/**
 * Encrypts a string.
 */
public interface StringEncrypter {
	String encrypt(String value);
	String decrypt(String value);
}
