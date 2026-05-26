package com.erumpay.card.domain.entity;

import com.erumpay.card.domain.enums.CardBenefitUsageStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "card_benefit_usage")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardBenefitUsage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "usage_id")
	private Long usageId;

	@Column(name = "payment_id", nullable = false)
	private Long paymentId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "card_id", nullable = false)
	private Long cardId;

	@Column(name = "benefit_id", nullable = false)
	private Long benefitId;

	@Column(name = "tier_id", nullable = false)
	private Long tierId;

	@Column(name = "approved_amount", nullable = false)
	private Integer approvedAmount;

	@Column(name = "benefit_amount", nullable = false)
	private Integer benefitAmount;

	@Column(name = "approved_at", nullable = false)
	private LocalDateTime approvedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private CardBenefitUsageStatus status;

	@Column(name = "canceled_at")
	private LocalDateTime canceledAt;

	@Column(name = "created_at", insertable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private LocalDateTime updatedAt;
}
