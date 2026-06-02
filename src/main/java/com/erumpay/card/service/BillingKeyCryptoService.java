package com.erumpay.card.service;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BillingKeyCryptoService {

	private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
	private static final String KEY_ALGORITHM = "AES";
	private static final String ENCRYPTED_PREFIX = "enc:v1:";

	private final String secretKey;
	private SecretKeySpec keySpec;

	public BillingKeyCryptoService(@Value("${card.billing-key.aes-key}") String secretKey) {
		this.secretKey = secretKey;
	}

	@PostConstruct
	void init() {
		byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
			throw new IllegalStateException("card.billing-key.aes-key must be 16, 24, or 32 bytes");
		}
		keySpec = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
	}

	public String encrypt(String billingKey) {
		if (billingKey == null || billingKey.isBlank()) {
			throw new IllegalArgumentException("billing key is required");
		}

		try {
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, initializedKeySpec());
			byte[] encrypted = cipher.doFinal(billingKey.getBytes(StandardCharsets.UTF_8));
			return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(encrypted);
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("billing key encryption failed", exception);
		}
	}

	public String decrypt(String storedBillingKey) {
		if (storedBillingKey == null || storedBillingKey.isBlank() || !isEncrypted(storedBillingKey)) {
			return storedBillingKey;
		}

		try {
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, initializedKeySpec());
			byte[] encrypted = Base64.getDecoder().decode(storedBillingKey.substring(ENCRYPTED_PREFIX.length()));
			return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
		} catch (IllegalArgumentException | GeneralSecurityException exception) {
			throw new IllegalStateException("billing key decryption failed", exception);
		}
	}

	private boolean isEncrypted(String billingKey) {
		return billingKey.startsWith(ENCRYPTED_PREFIX);
	}

	private SecretKeySpec initializedKeySpec() {
		if (keySpec == null) {
			throw new IllegalStateException("billing key crypto service is not initialized");
		}
		return keySpec;
	}
}
