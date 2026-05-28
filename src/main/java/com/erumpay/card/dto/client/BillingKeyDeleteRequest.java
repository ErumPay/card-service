package com.erumpay.card.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BillingKeyDeleteRequest(
	@JsonProperty("pay_card_id") Long payCardId,
	@JsonProperty("billing_key") String billingKey
) {
	@Override
	public String toString() {
		return "BillingKeyDeleteRequest(payCardId=%s, billingKey=****)".formatted(payCardId);
	}
}
