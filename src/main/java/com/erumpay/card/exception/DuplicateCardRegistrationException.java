package com.erumpay.card.exception;

import org.springframework.http.HttpStatus;

public class DuplicateCardRegistrationException extends CardServiceException {

	public DuplicateCardRegistrationException() {
		super(HttpStatus.CONFLICT, "DUPLICATE_CARD_REGISTRATION", "이미 등록된 카드 상품입니다.");
	}
}
