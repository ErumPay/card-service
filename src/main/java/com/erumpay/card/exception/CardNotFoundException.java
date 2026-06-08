package com.erumpay.card.exception;

public class CardNotFoundException extends CardServiceException {

	public CardNotFoundException() {
		super(ErrorCode.CARD_NOT_FOUND);
	}
}
