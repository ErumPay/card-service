package com.erumpay.card.exception;

import org.springframework.http.HttpStatus;

public class AuthServiceUnavailableException extends CardServiceException {

	public AuthServiceUnavailableException(Throwable cause) {
		super(HttpStatus.SERVICE_UNAVAILABLE, "AUTH_SERVICE_UNAVAILABLE", "회원 정보 조회를 일시적으로 사용할 수 없습니다.", cause);
	}
}
