package com.erumpay.card.dto;

import lombok.Getter;

@Getter
public class PaymentAvailabilityResponse {

	private final boolean available;
	private final String reason;

	private PaymentAvailabilityResponse(boolean available, String reason) {
		this.available = available;
		this.reason = reason;
	}

	public static PaymentAvailabilityResponse available() {
		return new PaymentAvailabilityResponse(true, null);
	}

	public static PaymentAvailabilityResponse unavailable(String reason) {
		return new PaymentAvailabilityResponse(false, reason);
	}
}
