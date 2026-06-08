package com.erumpay.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BillingKeyCryptoServiceTest {

	@Test
	void encryptReturnsCipherTextAndDecryptsToOriginalBillingKey() {
		BillingKeyCryptoService service = billingKeyCryptoService("0123456789abcdef");

		String encrypted = service.encrypt("billing-key");

		assertThat(encrypted).startsWith("enc:v1:");
		assertThat(encrypted).isNotEqualTo("billing-key");
		assertThat(service.decrypt(encrypted)).isEqualTo("billing-key");
	}

	@Test
	void decryptReturnsPlainValueWhenStoredBillingKeyIsLegacyPlainText() {
		BillingKeyCryptoService service = billingKeyCryptoService("0123456789abcdef");

		assertThat(service.decrypt("legacy-billing-key")).isEqualTo("legacy-billing-key");
	}

	@Test
	void encryptRejectsNullOrBlankBillingKey() {
		BillingKeyCryptoService service = billingKeyCryptoService("0123456789abcdef");

		assertThatThrownBy(() -> service.encrypt(null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("billing key is required");
		assertThatThrownBy(() -> service.encrypt(""))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("billing key is required");
		assertThatThrownBy(() -> service.encrypt(" "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("billing key is required");
	}

	@Test
	void decryptReturnsNullOrBlankValueAsIs() {
		BillingKeyCryptoService service = billingKeyCryptoService("0123456789abcdef");

		assertThat(service.decrypt(null)).isNull();
		assertThat(service.decrypt("")).isEmpty();
		assertThat(service.decrypt(" ")).isEqualTo(" ");
	}

	@Test
	void decryptFailsWhenEncryptedPayloadIsInvalid() {
		BillingKeyCryptoService service = billingKeyCryptoService("0123456789abcdef");

		assertThatThrownBy(() -> service.decrypt("enc:v1:invalid-base64"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("billing key decryption failed");
	}

	@Test
	void initFailsWhenKeyLengthIsInvalid() {
		BillingKeyCryptoService service = new BillingKeyCryptoService("short");

		assertThatThrownBy(service::init)
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("card.billing-key.aes-key must be 16, 24, or 32 bytes");
	}

	private BillingKeyCryptoService billingKeyCryptoService(String secretKey) {
		BillingKeyCryptoService service = new BillingKeyCryptoService(secretKey);
		service.init();
		return service;
	}
}
