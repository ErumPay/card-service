package com.erumpay.card.exception;

public class CardInformationInvalidException extends CardServiceException {

	public CardInformationInvalidException() {
		super(ErrorCode.CARD_INFORMATION_INVALID);
	}
}
