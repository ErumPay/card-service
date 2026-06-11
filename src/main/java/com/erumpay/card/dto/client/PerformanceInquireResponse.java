package com.erumpay.card.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PerformanceInquireResponse(
	@JsonProperty("card_company") String cardCompany,
	@JsonProperty("product_name") String productName,
	@JsonProperty("inquiry_period") String inquiryPeriod,
	@JsonProperty("current_amount") Long currentAmount,
	@JsonProperty("response_http") Integer responseHttp,
	@JsonProperty("response_code") String responseCode,
	@JsonProperty("response_reason") String responseReason,
	@JsonProperty("response_message") String responseMessage
) {
}
