package com.erumpay.card.exception;

import org.springframework.http.HttpStatus;

public class BillingKeyNotIntegratedException extends CardServiceException {

	public BillingKeyNotIntegratedException() {
		super(
			HttpStatus.NOT_IMPLEMENTED,
			"BILLING_KEY_NOT_INTEGRATED",
			"billing-key-service 연동 후 카드 등록이 가능합니다."
		);
	}
}
