package com.erumpay.card.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class InternalBillingKeyResponse {

	@ToString.Include
	private Long cardId;

	@ToString.Include
	private Long userId;

	@ToString.Include
	private Long cardProductId;

	@ToString.Include
	private String cardName;

	private String billingKey;

	private String maskedNumber;
}
