package com.erumpay.card.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BillingKeyDeleteResponse(
	@JsonProperty("pay_card_id") Long payCardId,
	@JsonProperty("billing_key") String billingKey,
	@JsonProperty("response_code") String responseCode,
	@JsonProperty("response_message") String responseMessage
) {
	@Override
	public String toString() {
		return "BillingKeyDeleteResponse(payCardId=%s, billingKey=****, responseCode=%s, responseMessage=%s)"
			.formatted(payCardId, responseCode, responseMessage);
	}
}
