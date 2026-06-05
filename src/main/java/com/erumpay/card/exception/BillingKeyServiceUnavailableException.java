package com.erumpay.card.exception;

public class BillingKeyServiceUnavailableException extends CardServiceException {

	public BillingKeyServiceUnavailableException(Throwable cause) {
		super(ErrorCode.BILLING_KEY_SERVICE_UNAVAILABLE, cause);
	}
}
