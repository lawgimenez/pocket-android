package com.pocket.sync.value.protect;

import android.content.Context;
import android.util.Base64;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.Config;
import com.google.crypto.tink.aead.AeadFactory;
import com.google.crypto.tink.aead.AeadKeyTemplates;
import com.google.crypto.tink.config.TinkConfig;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Encryption powered by Google's Tink library https://github.com/google/tink
 * With help from their sample Android app https://github.com/google/tink/tree/master/examples/helloworld/android
 */
public class TinkEncrypter implements StringEncrypter {
	
	private static final byte[] EMPTY_ASSOCIATED_DATA = new byte[0];
	
	private final String name;
	private final Context context;
	
	private Aead aead;
	
	public TinkEncrypter(Context context, String name) {
		this.name = "tkobsf_"+name;
		this.context = context;
	}
	
	private Aead getKeyStore() throws GeneralSecurityException, IOException {
		if (aead == null) {
			Config.register(TinkConfig.LATEST);
			aead = AeadFactory.getPrimitive(
					new AndroidKeysetManager.Builder()
					.withSharedPref(context, name, name)
					.doNotUseKeystore() // TODO This library is great in that it can use Android's Keystore protections on API levels that support it. However in testing, I've found that it could lose its key and then it couldn't recover the values it encrypted, which would be very problematic for us. So I've turned it off for the time being until we can understand this issue more. Based on research I've done, it does seem like standard practice is to just store access tokens in shared preferences and rely on the sandbox protections, but this takes it a step further and encrypts it. It is only a layer of obfuscation because the encryption key is stored in a different prefs file.
					.withKeyTemplate(AeadKeyTemplates.AES128_CTR_HMAC_SHA256) // Context of why we use this one https://pocket.slack.com/archives/CN0P0E7AM/p1569501025012000
					.withMasterKeyUri("android-keystore://"+name)
					.build()
					.getKeysetHandle());
		}
		return aead;
	}
	
	@Override
	public String encrypt(String value) {
		if (value == null) return null;
		
		try {
			byte[] plaintext = value.getBytes("UTF-8");
			byte[] ciphertext = getKeyStore().encrypt(plaintext, EMPTY_ASSOCIATED_DATA);
			return base64Encode(ciphertext);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public String decrypt(String value) {
		if (value == null) return null;
		
		try {
			byte[] ciphertext = base64Decode(value);
			byte[] plaintext = getKeyStore().decrypt(ciphertext, EMPTY_ASSOCIATED_DATA);
			return new String(plaintext, "UTF-8");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public boolean isAvailable() {
		return true;
	}
	
	private static String base64Encode(final byte[] input) {
		return Base64.encodeToString(input, Base64.DEFAULT);
	}
	
	private static byte[] base64Decode(String input) {
		return Base64.decode(input, Base64.DEFAULT);
	}
	
}
