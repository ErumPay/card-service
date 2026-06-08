package com.erumpay.card.dto;

import com.erumpay.card.domain.enums.PaymentUsageEventType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.List;

public record PaymentUsageEventRequest(
	@NotNull(message = "paymentId is required")
	Long paymentId,
	@NotNull(message = "eventType is required")
	PaymentUsageEventType eventType,
	@NotNull(message = "occurredAt is required")
	LocalDateTime occurredAt,
	@NotEmpty(message = "cards must not be empty")
	List<@Valid PaymentUsageCardRequest> cards
) {

	public record PaymentUsageCardRequest(
		@NotNull(message = "paymentCardId is required")
		Long paymentCardId,
		@NotNull(message = "cardId is required")
		Long cardId,
		@NotNull(message = "approvedAmount is required")
		@Positive(message = "approvedAmount must be positive")
		Long approvedAmount,
		LocalDateTime approvedAt,
		@Valid
		AppliedBenefitRequest appliedBenefit
	) {
	}

	public record AppliedBenefitRequest(
		@NotNull(message = "benefitId is required")
		Long benefitId,
		@NotNull(message = "tierId is required")
		Long tierId,
		@NotNull(message = "benefitAmount is required")
		@Positive(message = "benefitAmount must be positive")
		Long benefitAmount
	) {
	}
}
