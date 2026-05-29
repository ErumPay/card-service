package com.erumpay.card.exception;

import org.springframework.http.HttpStatus;

public class BillingKeyIssuePendingException extends CardServiceException {

	public BillingKeyIssuePendingException() {
		super(HttpStatus.CONFLICT, "BILLING_KEY_ISSUE_PENDING", "빌링키 발급이 아직 완료되지 않았습니다.");
	}
}
