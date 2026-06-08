package com.erumpay.card.exception;

public class InvalidPaymentUsageEventRequestException extends CardServiceException {

	private final String detail;

	public InvalidPaymentUsageEventRequestException(String message) {
		super(ErrorCode.INVALID_PAYMENT_USAGE_EVENT_REQUEST);
		this.detail = message;
	}

	public String getDetail() {
		return detail;
	}
}
