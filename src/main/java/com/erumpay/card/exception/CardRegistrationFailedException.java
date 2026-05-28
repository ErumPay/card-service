package com.erumpay.card.exception;

import org.springframework.http.HttpStatus;

public class CardRegistrationFailedException extends CardServiceException {

	public CardRegistrationFailedException(Throwable cause) {
		super(HttpStatus.INTERNAL_SERVER_ERROR, "CARD_REGISTRATION_FAILED", "카드 등록에 실패했습니다.", cause);
	}
}
