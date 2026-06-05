package com.erumpay.card.exception;

public class BillingKeyIssueFailedException extends CardServiceException {

	public BillingKeyIssueFailedException() {
		super(ErrorCode.BILLING_KEY_ISSUE_FAILED);
	}

	public BillingKeyIssueFailedException(Throwable cause) {
		super(ErrorCode.BILLING_KEY_ISSUE_FAILED, cause);
	}
}
