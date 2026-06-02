package com.erumpay.card.exception;

import org.springframework.http.HttpStatus;

public class InvalidBillingKeyLookupRequestException extends CardServiceException {

	public InvalidBillingKeyLookupRequestException() {
		super(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "cardIds 요청 형식 오류");
	}
}
