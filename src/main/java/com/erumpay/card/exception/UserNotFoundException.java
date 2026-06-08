package com.erumpay.card.exception;

public class UserNotFoundException extends CardServiceException {

	public UserNotFoundException() {
		super(ErrorCode.USER_NOT_FOUND);
	}
}
