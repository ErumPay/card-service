package com.erumpay.card.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardPerformanceResponse {

	private Long cardId;
	private String yearMonth;
	private Long amount;
	private Long discountAmount;
	private Long targetAmount;
}
