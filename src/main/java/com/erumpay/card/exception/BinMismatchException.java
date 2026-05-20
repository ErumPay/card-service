package com.erumpay.card.exception;

import org.springframework.http.HttpStatus;

public class BinMismatchException extends CardServiceException {

	public BinMismatchException() {
		super(HttpStatus.BAD_REQUEST, "BIN_MISMATCH", "카드번호와 Mock BIN이 일치하지 않습니다.");
	}
}
