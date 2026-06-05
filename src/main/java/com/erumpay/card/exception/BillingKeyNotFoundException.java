package com.erumpay.card.exception;

public class BillingKeyNotFoundException extends CardServiceException {

	public BillingKeyNotFoundException() {
		super(ErrorCode.BILLING_KEY_NOT_FOUND);
	}
}
