package com.erumpay.card.exception;

public class InvalidExpiryYmException extends CardServiceException {

	public InvalidExpiryYmException() {
		super(ErrorCode.INVALID_EXPIRY_YM);
	}

	public InvalidExpiryYmException(Throwable cause) {
		super(ErrorCode.INVALID_EXPIRY_YM, cause);
	}
}
