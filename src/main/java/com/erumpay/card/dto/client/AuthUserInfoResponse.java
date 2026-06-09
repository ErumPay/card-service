package com.erumpay.card.dto.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthUserInfoResponse(
	Long userId,
	String name,
	String phoneNumber,
	String birthDate,
	String status
) {
	public AuthUserInfoResponse(Long userId, String birthDate, String status) {
		this(userId, null, null, birthDate, status);
	}

	@Override
	public String toString() {
		return "AuthUserInfoResponse(userId=%s, name=%s, phoneNumber=****, birthDate=****, status=%s)"
			.formatted(userId, name, status);
	}
}
