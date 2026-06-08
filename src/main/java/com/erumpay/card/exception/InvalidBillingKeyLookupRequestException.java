package com.erumpay.card.exception;

public class InvalidBillingKeyLookupRequestException extends CardServiceException {

	public InvalidBillingKeyLookupRequestException() {
		super(ErrorCode.INVALID_REQUEST, "cardIds 요청 형식 오류");
	}
}
