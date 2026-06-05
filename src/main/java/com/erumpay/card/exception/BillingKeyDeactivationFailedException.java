package com.erumpay.card.exception;

public class BillingKeyDeactivationFailedException extends CardServiceException {

	public BillingKeyDeactivationFailedException() {
		super(ErrorCode.BILLING_KEY_DEACTIVATION_FAILED);
	}

	public BillingKeyDeactivationFailedException(Throwable cause) {
		super(ErrorCode.BILLING_KEY_DEACTIVATION_FAILED, cause);
	}
}
