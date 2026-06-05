package com.erumpay.card.dto.event;

import java.time.LocalDateTime;

public record CardNotificationEventMessage(
	String eventId,
	String eventType,
	Long userId,
	String title,
	String content,
	Long paymentId,
	LocalDateTime occurredAt,
	String correlationId
) {
}
