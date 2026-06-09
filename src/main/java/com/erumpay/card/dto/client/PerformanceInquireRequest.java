package com.erumpay.card.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PerformanceInquireRequest(
	String name,
	@JsonProperty("phone_number") String phoneNumber,
	@JsonProperty("card_company") String cardCompany,
	@JsonProperty("product_name") String productName,
	@JsonProperty("inquiry_period") String inquiryPeriod
) {
}
