package com.erumpay.card.exception;

import org.springframework.http.HttpStatus;

public class BillingKeyIssueFailedException extends CardServiceException {

	public BillingKeyIssueFailedException() {
		super(HttpStatus.BAD_GATEWAY, "BILLING_KEY_ISSUE_FAILED", "빌링키 발급에 실패했습니다.");
	}
}
