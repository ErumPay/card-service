package com.erumpay.card.dto;

import java.math.BigDecimal;
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
		private String imageUrl;
		private String maskedNumber;
		private Boolean isDefault;
		private Long performanceAmount;
		private Long previousMonthPerformanceAmount;
		private Long currentMonthPerformanceAmount;
		private List<InternalRecommendationBenefitResponse> benefits;
	}

	@Getter
	@Builder
	public static class InternalRecommendationBenefitResponse {

		private Long benefitId;
		private String serviceCategory;
		private String benefitType;
		private Long minAmount;
		private String timeStart;
		private String timeEnd;
		private String dayCondition;
		private String benefitDesc;
		private List<String> brandNames;
		private InternalBenefitUsageResponse usage;
		private List<InternalRecommendationBenefitTierResponse> tiers;
	}

	@Getter
	@Builder
	public static class InternalBenefitUsageResponse {

		private Long dailyAmount;
		private Long dailyCount;
		private Long monthlyAmount;
		private Long monthlyCount;
		private Long yearlyAmount;
		private Long yearlyCount;
	}

	@Getter
	@Builder
	public static class InternalRecommendationBenefitTierResponse {

		private Long tierId;
		private Long minPrevMonthUsage;
		private Long maxPrevMonthUsage;
		private BigDecimal rate;
		private Long flatAmount;
		private Long maxBenefitPerUse;
		private Integer dailyLimitCount;
		private Long dailyLimitAmount;
		private Integer monthlyLimitCount;
		private Long monthlyLimitAmount;
		private Integer yearlyLimitCount;
		private Long yearlyLimitAmount;
		private String tierDesc;
	}
}
