package com.erumpay.card.exception;

import org.springframework.http.HttpStatus;

public class CardNotActiveException extends CardServiceException {

	public CardNotActiveException() {
		super(HttpStatus.CONFLICT, "CARD_NOT_ACTIVE", "활성 상태의 카드만 처리할 수 있습니다.");
	}
}
