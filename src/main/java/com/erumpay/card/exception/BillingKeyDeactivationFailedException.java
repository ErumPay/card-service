package com.erumpay.card.exception;

import org.springframework.http.HttpStatus;

public class BillingKeyDeactivationFailedException extends CardServiceException {

	public BillingKeyDeactivationFailedException() {
		super(HttpStatus.BAD_GATEWAY, "BILLING_KEY_DEACTIVATION_FAILED", "빌링키 비활성화에 실패했습니다.");
	}
}
