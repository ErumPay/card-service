package com.erumpay.card.exception;

public class CardProductNotFoundException extends CardServiceException {

	public CardProductNotFoundException() {
		super(ErrorCode.CARD_PRODUCT_NOT_FOUND);
	}
}
