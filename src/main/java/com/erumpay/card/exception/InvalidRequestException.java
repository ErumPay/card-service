package com.erumpay.card.exception;

public class InvalidRequestException extends CardServiceException {

	public InvalidRequestException() {
		super(ErrorCode.INVALID_REQUEST);
	}

	public InvalidRequestException(Throwable cause) {
		super(ErrorCode.INVALID_REQUEST, cause);
	}
}
