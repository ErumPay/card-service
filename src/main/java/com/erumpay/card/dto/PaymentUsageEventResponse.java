package com.erumpay.card.dto;

import com.erumpay.card.domain.enums.PaymentUsageEventType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentUsageEventResponse {

	private Long paymentId;
	private PaymentUsageEventType eventType;
	private boolean applied;
	private int appliedCardCount;
	private String reason;
}
