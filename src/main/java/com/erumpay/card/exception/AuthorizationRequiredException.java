package com.erumpay.card.exception;

public class AuthorizationRequiredException extends CardServiceException {

	public AuthorizationRequiredException() {
		super(ErrorCode.AUTHORIZATION_REQUIRED);
	}
}
