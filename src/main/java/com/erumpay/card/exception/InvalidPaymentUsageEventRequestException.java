package com.erumpay.card.exception;

import org.springframework.http.HttpStatus;

public class InvalidPaymentUsageEventRequestException extends CardServiceException {

	public InvalidPaymentUsageEventRequestException(String message) {
		super(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message);
	}
}
