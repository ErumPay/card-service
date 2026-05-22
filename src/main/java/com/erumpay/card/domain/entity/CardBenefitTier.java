package com.erumpay.card.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "card_benefit_tier")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardBenefitTier {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "tier_id")
	private Long tierId;

	@Column(name = "benefit_id", nullable = false)
	private Long benefitId;

	@Column(name = "min_prev_month_usage", nullable = false)
	private Long minPrevMonthUsage;

	@Column(name = "max_prev_month_usage")
	private Long maxPrevMonthUsage;

	@Column(name = "rate", precision = 5, scale = 3)
	private BigDecimal rate;

	@Column(name = "flat_amount")
	private Long flatAmount;

	@Column(name = "max_benefit_per_use")
	private Long maxBenefitPerUse;

	@Column(name = "daily_limit_count")
	private Integer dailyLimitCount;

	@Column(name = "daily_limit_amount")
	private Long dailyLimitAmount;

	@Column(name = "monthly_limit_count")
	private Integer monthlyLimitCount;

	@Column(name = "monthly_limit_amount")
	private Long monthlyLimitAmount;

	@Column(name = "yearly_limit_count")
	private Integer yearlyLimitCount;

	@Column(name = "yearly_limit_amount")
	private Long yearlyLimitAmount;

	@Column(name = "tier_desc", length = 500)
	private String tierDesc;

	@Column(name = "created_at", insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private LocalDateTime updatedAt;
}
