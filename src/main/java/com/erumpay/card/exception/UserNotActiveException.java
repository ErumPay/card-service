package com.erumpay.card.exception;

public class UserNotActiveException extends CardServiceException {

	public UserNotActiveException() {
		super(ErrorCode.USER_NOT_ACTIVE);
	}
}
