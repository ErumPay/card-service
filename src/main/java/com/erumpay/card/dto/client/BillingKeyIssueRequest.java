package com.erumpay.card.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BillingKeyIssueRequest(
	@JsonProperty("pay_card_id") Long payCardId,
	@JsonProperty("card_number") String cardNumber,
	@JsonProperty("expiry_date") String expiryDate,
	@JsonProperty("cvc") String cvc,
	@JsonProperty("password_2digit") String password2digit,
	@JsonProperty("birth_date") String birthDate
) {
	@Override
	public String toString() {
		return "BillingKeyIssueRequest(payCardId=%s, cardNumber=****, expiryDate=****, cvc=****, password2digit=****, birthDate=****)"
			.formatted(payCardId);
	}
}
