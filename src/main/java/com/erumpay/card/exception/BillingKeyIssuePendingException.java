package com.erumpay.card.exception;

public class BillingKeyIssuePendingException extends CardServiceException {

	public BillingKeyIssuePendingException() {
		super(ErrorCode.BILLING_KEY_ISSUE_PENDING);
	}
}
