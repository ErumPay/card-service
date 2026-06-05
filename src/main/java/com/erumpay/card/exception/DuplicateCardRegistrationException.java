package com.erumpay.card.exception;

public class DuplicateCardRegistrationException extends CardServiceException {

	public DuplicateCardRegistrationException() {
		super(ErrorCode.DUPLICATE_CARD_REGISTRATION);
	}
}
