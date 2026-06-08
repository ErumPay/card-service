package com.erumpay.card.exception;

public class CardNotActiveException extends CardServiceException {

	public CardNotActiveException() {
		super(ErrorCode.CARD_NOT_ACTIVE);
	}
}
