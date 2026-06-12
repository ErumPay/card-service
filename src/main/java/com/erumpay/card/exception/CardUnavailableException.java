package com.erumpay.card.exception;

public class CardUnavailableException extends CardServiceException {

	public CardUnavailableException() {
		super(ErrorCode.CARD_UNAVAILABLE);
	}
}
