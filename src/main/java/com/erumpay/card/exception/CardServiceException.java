package com.erumpay.card.exception;

public abstract class CardServiceException extends RuntimeException {

	private final ErrorCode errorCode;

	protected CardServiceException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	protected CardServiceException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	protected CardServiceException(ErrorCode errorCode, String message, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
	}

	protected CardServiceException(ErrorCode errorCode, Throwable cause) {
		super(errorCode.getMessage(), cause);
		this.errorCode = errorCode;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}

	public org.springframework.http.HttpStatus getStatus() {
		return errorCode.getStatus();
	}

	public String getCode() {
		return errorCode.getCode();
	}

	public String getReason() {
		return errorCode.getReason();
	}
}
