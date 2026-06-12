package com.erumpay.card.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "CARD-REQ-001", "INVALID_REQUEST", "잘못된 요청입니다."),
	INVALID_EXPIRY_YM(HttpStatus.BAD_REQUEST, "CARD-REQ-002", "INVALID_EXPIRY_YM", "유효기간 형식이 올바르지 않습니다."),
	INVALID_YEAR_MONTH(HttpStatus.BAD_REQUEST, "CARD-REQ-003", "INVALID_YEAR_MONTH", "연월 형식이 올바르지 않습니다."),
	AUTHORIZATION_REQUIRED(HttpStatus.UNAUTHORIZED, "CARD-AUTH-100", "AUTHORIZATION_REQUIRED", "인증 정보가 필요합니다."),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "CARD-AUTH-201", "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
	USER_NOT_ACTIVE(HttpStatus.CONFLICT, "CARD-AUTH-203", "USER_NOT_ACTIVE", "활성 상태의 사용자만 카드를 등록할 수 있습니다."),
	INVALID_USER_BIRTH_DATE(
		HttpStatus.CONFLICT,
		"CARD-AUTH-204",
		"INVALID_USER_BIRTH_DATE",
		"사용자 생년월일 형식이 올바르지 않습니다."
	),
	AUTH_SERVICE_UNAVAILABLE(
		HttpStatus.SERVICE_UNAVAILABLE,
		"CARD-AUTH-400",
		"AUTH_SERVICE_UNAVAILABLE",
		"회원 정보 조회를 일시적으로 사용할 수 없습니다."
	),
	CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "CARD-CARD-201", "CARD_NOT_FOUND", "카드를 찾을 수 없습니다."),
	CARD_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "CARD-CARD-202", "CARD_PRODUCT_NOT_FOUND", "지원하지 않는 카드입니다."),
	CARD_NOT_ACTIVE(HttpStatus.CONFLICT, "CARD-CARD-203", "CARD_NOT_ACTIVE", "활성 상태의 카드만 처리할 수 있습니다."),
	CARD_UNAVAILABLE(
		HttpStatus.CONFLICT,
		"CARD-CARD-303",
		"CARD_UNAVAILABLE",
		"만료되었거나 사용할 수 없는 카드입니다."
	),
	CARD_INFORMATION_INVALID(
		HttpStatus.BAD_REQUEST,
		"CARD-CARD-304",
		"CARD_INFORMATION_INVALID",
		"입력한 카드 정보가 올바르지 않습니다."
	),
	CARD_AUTHENTICATION_FAILED(
		HttpStatus.UNAUTHORIZED,
		"CARD-AUTH-301",
		"CARD_AUTHENTICATION_FAILED",
		"카드 비밀번호 인증에 실패했습니다."
	),
	DUPLICATE_CARD_REGISTRATION(
		HttpStatus.CONFLICT,
		"CARD-CARD-301",
		"DUPLICATE_CARD_REGISTRATION",
		"이미 등록된 카드 상품입니다."
	),
	CARD_REGISTRATION_FAILED(
		HttpStatus.INTERNAL_SERVER_ERROR,
		"CARD-CARD-901",
		"CARD_REGISTRATION_FAILED",
		"카드 등록에 실패했습니다."
	),
	BILLING_KEY_NOT_FOUND(HttpStatus.CONFLICT, "CARD-BILL-201", "BILLING_KEY_NOT_FOUND", "빌링키를 찾을 수 없습니다."),
	BILLING_KEY_ISSUE_PENDING(
		HttpStatus.CONFLICT,
		"CARD-BILL-203",
		"BILLING_KEY_ISSUE_PENDING",
		"빌링키 발급이 아직 완료되지 않았습니다."
	),
	BILLING_KEY_ISSUE_FAILED(
		HttpStatus.BAD_GATEWAY,
		"CARD-BILL-400",
		"BILLING_KEY_ISSUE_FAILED",
		"빌링키 발급에 실패했습니다."
	),
	BILLING_KEY_DEACTIVATION_FAILED(
		HttpStatus.BAD_GATEWAY,
		"CARD-BILL-401",
		"BILLING_KEY_DEACTIVATION_FAILED",
		"빌링키 비활성화에 실패했습니다."
	),
	BILLING_KEY_SERVICE_UNAVAILABLE(
		HttpStatus.SERVICE_UNAVAILABLE,
		"CARD-BILL-402",
		"BILLING_KEY_SERVICE_UNAVAILABLE",
		"빌링키 서비스를 사용할 수 없습니다."
	),
	BILLING_KEY_ISSUE_UNKNOWN(
		HttpStatus.SERVICE_UNAVAILABLE,
		"CARD-BILL-403",
		"BILLING_KEY_ISSUE_UNKNOWN",
		"빌링키 발급 결과를 확인할 수 없습니다."
	),
	INVALID_PAYMENT_USAGE_EVENT_REQUEST(
		HttpStatus.BAD_REQUEST,
		"CARD-USG-001",
		"INVALID_PAYMENT_USAGE_EVENT_REQUEST",
		"결제 사용량 이벤트 요청이 올바르지 않습니다."
	),
	INTERNAL_SERVER_ERROR(
		HttpStatus.INTERNAL_SERVER_ERROR,
		"CARD-SYS-900",
		"INTERNAL_SERVER_ERROR",
		"알 수 없는 내부 오류가 발생했습니다."
	);

	private final HttpStatus status;
	private final String code;
	private final String reason;
	private final String message;

	ErrorCode(HttpStatus status, String code, String reason, String message) {
		this.status = status;
		this.code = code;
		this.reason = reason;
		this.message = message;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}

	public String getReason() {
		return reason;
	}

	public String getMessage() {
		return message;
	}
}
