package com.erumpay.card.exception;

import org.springframework.http.HttpStatus;

public class BillingKeyServiceUnavailableException extends CardServiceException {

	public BillingKeyServiceUnavailableException(Throwable cause) {
		super(
			HttpStatus.SERVICE_UNAVAILABLE,
			"BILLING_KEY_SERVICE_UNAVAILABLE",
			"빌링키 서비스를 사용할 수 없습니다.",
			cause
		);
	}
}
