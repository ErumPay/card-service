package com.erumpay.card.exception;

import org.springframework.http.HttpStatus;

public class InvalidExpiryYmException extends CardServiceException {

	public InvalidExpiryYmException() {
		super(HttpStatus.BAD_REQUEST, "INVALID_EXPIRY_YM", "유효기간 형식 오류");
	}

	public InvalidExpiryYmException(Throwable cause) {
		super(HttpStatus.BAD_REQUEST, "INVALID_EXPIRY_YM", "유효기간 형식 오류", cause);
	}
}
