package com.erumpay.card.exception;

public class InvalidYearMonthException extends CardServiceException {

	public InvalidYearMonthException() {
		super(ErrorCode.INVALID_YEAR_MONTH);
	}
}
