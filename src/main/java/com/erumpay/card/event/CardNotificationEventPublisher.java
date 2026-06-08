package com.erumpay.card.event;

import com.erumpay.card.dto.event.CardNotificationEventMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardNotificationEventPublisher {

	private static final String CARD_REGISTERED = "CARD_REGISTERED";
	private static final String CARD_DELETED = "CARD_DELETED";
	private static final String REGISTERED_TITLE = "카드 등록 완료";
	private static final String DELETED_TITLE = "카드 삭제 완료";

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	@Value("${card.notification.topic:card.event}")
	private String topic;

	public void publishRegistered(Long userId, Long cardId, String cardName) {
		userId = requireNonNull(userId, "userId");
		cardId = requireNonNull(cardId, "cardId");
		cardName = requireNonNull(cardName, "cardName");

		publish(
			new CardNotificationEventMessage(
				eventId(cardId, "registered", userId),
				CARD_REGISTERED,
				userId,
				REGISTERED_TITLE,
				cardName + "가 등록되었습니다.",
				null,
				LocalDateTime.now(clock),
				correlationId()
			)
		);
	}

	public void publishDeleted(Long userId, Long cardId, String cardName) {
		userId = requireNonNull(userId, "userId");
		cardId = requireNonNull(cardId, "cardId");
		cardName = requireNonNull(cardName, "cardName");

		publish(
			new CardNotificationEventMessage(
				eventId(cardId, "deleted", userId),
				CARD_DELETED,
				userId,
				DELETED_TITLE,
				cardName + "가 삭제되었습니다.",
				null,
				LocalDateTime.now(clock),
				correlationId()
			)
		);
	}

	private void publish(CardNotificationEventMessage event) {
		try {
			String payload = objectMapper.writeValueAsString(event);
			kafkaTemplate.send(topic, event.userId().toString(), payload)
				.whenComplete((result, exception) -> {
					if (exception != null) {
						log.warn(
							"Failed to publish card notification event. eventId={}, eventType={}, userId={}, correlationId={}",
							event.eventId(),
							event.eventType(),
							event.userId(),
							event.correlationId(),
							exception
						);
					}
				});
		} catch (JsonProcessingException exception) {
			log.warn(
				"Failed to serialize card notification event. eventId={}, eventType={}, userId={}, correlationId={}",
				event.eventId(),
				event.eventType(),
				event.userId(),
				event.correlationId(),
				exception
			);
		} catch (RuntimeException exception) {
			log.warn(
				"Failed to send card notification event. eventId={}, eventType={}, userId={}, correlationId={}",
				event.eventId(),
				event.eventType(),
				event.userId(),
				event.correlationId(),
				exception
			);
		}
	}

	private String eventId(Long cardId, String action, Long userId) {
		return "card:" + cardId + ":" + action + ":user:" + userId;
	}

	private String correlationId() {
		return "card_" + UUID.randomUUID();
	}

	private <T> T requireNonNull(T value, String fieldName) {
		if (value == null) {
			throw new IllegalArgumentException(fieldName + " must not be null");
		}
		return value;
	}
}
