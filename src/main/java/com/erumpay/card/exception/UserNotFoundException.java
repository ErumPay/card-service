package com.erumpay.card.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends CardServiceException {

	public UserNotFoundException() {
		super(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다.");
	}
}
