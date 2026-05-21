package com.erumpay.card.exception;

import org.springframework.http.HttpStatus;

public class CardProductNotFoundException extends CardServiceException {

	public CardProductNotFoundException() {
		super(HttpStatus.NOT_FOUND, "CARD_PRODUCT_NOT_FOUND", "지원하지 않는 카드입니다.");
	}
}
