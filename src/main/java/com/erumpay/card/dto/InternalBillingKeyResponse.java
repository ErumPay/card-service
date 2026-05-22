package com.erumpay.card.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class InternalBillingKeyResponse {

	private Long cardId;
	private Long userId;
	private Long cardProductId;

	@ToString.Exclude
	private String encryptedBillingKey;

	private String maskedNumber;
}
