package com.erumpay.card.exception;

import org.springframework.http.HttpStatus;

public class CardNotFoundException extends CardServiceException {

	public CardNotFoundException() {
		super(HttpStatus.NOT_FOUND, "CARD_NOT_FOUND", "카드를 찾을 수 없습니다.");
	}
}
