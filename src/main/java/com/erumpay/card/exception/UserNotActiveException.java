package com.erumpay.card.exception;

import org.springframework.http.HttpStatus;

public class UserNotActiveException extends CardServiceException {

	public UserNotActiveException() {
		super(HttpStatus.CONFLICT, "USER_NOT_ACTIVE", "활성 상태의 사용자만 카드를 등록할 수 있습니다.");
	}
}
