package com.erumpay.card.exception;

public class InvalidUserBirthDateException extends CardServiceException {

	public InvalidUserBirthDateException() {
		super(ErrorCode.INVALID_USER_BIRTH_DATE);
	}
}
