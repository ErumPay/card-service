package com.erumpay.card.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalDefaultCardResponse {

	private Long cardId;
	private Long userId;
	private Long cardProductId;
	private String maskedNumber;
	private String cardCompany;
	private String cardName;
}
