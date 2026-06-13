package com.erumpay.card.exception;

public class CardAuthenticationFailedException extends CardServiceException {

	public CardAuthenticationFailedException() {
		super(ErrorCode.CARD_AUTHENTICATION_FAILED);
	}
}
