package com.erumpay.card.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InternalRecommendationSourceResponse {

	private Long userId;
	private String yearMonth;
	private List<InternalRecommendationCardResponse> cards;

	@Getter
	@Builder
	public static class InternalRecommendationCardResponse {

		private Long cardId;
		private Long cardProductId;
		private String cardCompany;
		private String cardName;
		private String maskedNumber;
		private Boolean isDefault;
		private Long performanceAmount;
		private List<CardBenefitResponse> benefits;
	}
}
