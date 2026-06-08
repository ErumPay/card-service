package com.erumpay.card.exception;

public class AuthServiceUnavailableException extends CardServiceException {

	public AuthServiceUnavailableException(Throwable cause) {
		super(ErrorCode.AUTH_SERVICE_UNAVAILABLE, cause);
	}
}
