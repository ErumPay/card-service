package com.erumpay.card.dto.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthUserInfoResponse(
	Long userId,
	String birthDate,
	String status
) {
	@Override
	public String toString() {
		return "AuthUserInfoResponse(userId=%s, birthDate=****, status=%s)".formatted(userId, status);
	}
}
