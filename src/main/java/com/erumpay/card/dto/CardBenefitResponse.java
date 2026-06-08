package com.erumpay.card.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardBenefitResponse {

	private Long benefitId;
	private String serviceCategory;
	private String benefitType;
	private Long minAmount;
	private String timeStart;
	private String timeEnd;
	private String dayCondition;
	private String benefitDesc;
	private List<String> brandNames;
	private List<CardBenefitTierResponse> tiers;

	@Getter
	@Builder
	public static class CardBenefitTierResponse {

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
