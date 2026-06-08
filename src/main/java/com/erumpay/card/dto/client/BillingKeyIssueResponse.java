package com.erumpay.card.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BillingKeyIssueResponse(
	@JsonProperty("pay_card_id") Long payCardId,
	@JsonProperty("billing_key") String billingKey,
	@JsonProperty("masked_number") String maskedNumber,
	@JsonProperty("card_company") String cardCompany,
	@JsonProperty("response_code") String responseCode,
	@JsonProperty("response_message") String responseMessage
) {
	@Override
	public String toString() {
		return "BillingKeyIssueResponse(payCardId=%s, billingKey=****, maskedNumber=%s, cardCompany=%s, responseCode=%s, responseMessage=%s)"
			.formatted(payCardId, maskedNumber, cardCompany, responseCode, responseMessage);
	}
}
