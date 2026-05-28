package com.erumpay.card.exception;

import org.springframework.http.HttpStatus;

public class InvalidUserBirthDateException extends CardServiceException {

	public InvalidUserBirthDateException() {
		super(HttpStatus.CONFLICT, "INVALID_USER_BIRTH_DATE", "사용자 생년월일 형식이 올바르지 않습니다.");
	}
}
