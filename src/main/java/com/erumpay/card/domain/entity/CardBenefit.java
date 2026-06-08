package com.erumpay.card.domain.entity;

import com.erumpay.card.domain.enums.BenefitType;
import com.erumpay.card.domain.enums.DayCondition;
import com.erumpay.card.domain.enums.ServiceCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "card_benefit")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardBenefit {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "benefit_id")
	private Long benefitId;

	@Column(name = "card_product_id", nullable = false)
	private Long cardProductId;

	@Enumerated(EnumType.STRING)
	@Column(name = "service_category", nullable = false)
	private ServiceCategory serviceCategory;

	@Enumerated(EnumType.STRING)
	@Column(name = "benefit_type", nullable = false)
	private BenefitType benefitType;

	@Column(name = "min_amount")
	private Long minAmount;

	@Column(name = "time_start")
	private LocalTime timeStart;

	@Column(name = "time_end")
	private LocalTime timeEnd;

	@Enumerated(EnumType.STRING)
	@Column(name = "day_condition", nullable = false)
	private DayCondition dayCondition;

	@Column(name = "benefit_desc", length = 500)
	private String benefitDesc;

	@Column(name = "priority", nullable = false)
	private Integer priority;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	@Column(name = "created_at", insertable = false, updatable = false)
	private LocalDateTime createdAt;
}
