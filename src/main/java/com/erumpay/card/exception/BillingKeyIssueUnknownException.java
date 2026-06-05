package com.erumpay.card.exception;

public class BillingKeyIssueUnknownException extends CardServiceException {

	public BillingKeyIssueUnknownException(Throwable cause) {
		super(ErrorCode.BILLING_KEY_ISSUE_UNKNOWN, cause);
	}
}
