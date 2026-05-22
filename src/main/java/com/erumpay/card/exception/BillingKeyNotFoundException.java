package com.erumpay.card.exception;

import org.springframework.http.HttpStatus;

public class BillingKeyNotFoundException extends CardServiceException {

	public BillingKeyNotFoundException() {
		super(HttpStatus.CONFLICT, "BILLING_KEY_NOT_FOUND", "빌링키를 찾을 수 없습니다.");
	}
}
