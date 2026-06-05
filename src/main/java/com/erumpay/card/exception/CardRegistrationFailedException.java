package com.erumpay.card.exception;

public class CardRegistrationFailedException extends CardServiceException {

	public CardRegistrationFailedException(Throwable cause) {
		super(ErrorCode.CARD_REGISTRATION_FAILED, cause);
	}
}
