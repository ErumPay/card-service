package com.erumpay.card.exception;

import org.springframework.http.HttpStatus;

public class InvalidYearMonthException extends CardServiceException {

	public InvalidYearMonthException() {
		super(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "yearMonth 형식 오류");
	}
}
