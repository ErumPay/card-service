package com.erumpay.card.exception;

import org.springframework.http.HttpStatus;

public class BillingKeyIssueUnknownException extends CardServiceException {

	public BillingKeyIssueUnknownException(Throwable cause) {
		super(
			HttpStatus.SERVICE_UNAVAILABLE,
			"BILLING_KEY_ISSUE_UNKNOWN",
			"빌링키 발급 결과를 확인할 수 없습니다.",
			cause
		);
	}
}
