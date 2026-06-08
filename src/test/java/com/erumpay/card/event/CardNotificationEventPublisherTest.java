package com.erumpay.card.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CardNotificationEventPublisherTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(
		Instant.parse("2026-06-05T07:00:00Z"),
		ZoneId.of("Asia/Seoul")
	);

	@Mock
	private KafkaTemplate<String, String> kafkaTemplate;

	private CardNotificationEventPublisher publisher;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper()
			.registerModule(new JavaTimeModule())
			.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		publisher = new CardNotificationEventPublisher(kafkaTemplate, objectMapper, FIXED_CLOCK);
		ReflectionTestUtils.setField(publisher, "topic", "card.event");
	}

	@Test
	void publishRegisteredSendsStringJsonWithNotificationContract() throws Exception {
		when(kafkaTemplate.send(anyString(), anyString(), anyString()))
			.thenReturn(CompletableFuture.completedFuture(null));

		publisher.publishRegistered(101L, 3001L, "KB국민 My WE:SH 카드");

		ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
		verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), payloadCaptor.capture());

		JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
		assertThat(topicCaptor.getValue()).isEqualTo("card.event");
		assertThat(keyCaptor.getValue()).isEqualTo("101");
		assertThat(payload.get("eventId").asText()).isEqualTo("card:3001:registered:user:101");
		assertThat(payload.get("eventType").asText()).isEqualTo("CARD_REGISTERED");
		assertThat(payload.get("userId").asLong()).isEqualTo(101L);
		assertThat(payload.get("title").asText()).isEqualTo("카드 등록 완료");
		assertThat(payload.get("content").asText()).isEqualTo("KB국민 My WE:SH 카드가 등록되었습니다.");
		assertThat(payload.get("paymentId").isNull()).isTrue();
		assertThat(payload.get("occurredAt").asText()).isEqualTo("2026-06-05T16:00:00");
		assertThat(payload.get("correlationId").asText()).matches("^card_[0-9a-f\\-]{36}$");
		assertThat(payloadCaptor.getValue()).doesNotContain("billing-key", "8000001234567890");
	}

	@Test
	void publishDeletedSendsDeletedEvent() throws Exception {
		when(kafkaTemplate.send(anyString(), anyString(), anyString()))
			.thenReturn(CompletableFuture.completedFuture(null));

		publisher.publishDeleted(101L, 3001L, "KB국민 My WE:SH 카드");

		ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
		verify(kafkaTemplate).send(anyString(), anyString(), payloadCaptor.capture());

		JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
		assertThat(payload.get("eventId").asText()).isEqualTo("card:3001:deleted:user:101");
		assertThat(payload.get("eventType").asText()).isEqualTo("CARD_DELETED");
		assertThat(payload.get("title").asText()).isEqualTo("카드 삭제 완료");
		assertThat(payload.get("content").asText()).isEqualTo("KB국민 My WE:SH 카드가 삭제되었습니다.");
	}

	@Test
	void publishRegisteredRejectsNullInputs() {
		assertThatThrownBy(() -> publisher.publishRegistered(null, 3001L, "KB card"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("userId must not be null");
		assertThatThrownBy(() -> publisher.publishRegistered(101L, null, "KB card"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("cardId must not be null");
		assertThatThrownBy(() -> publisher.publishRegistered(101L, 3001L, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("cardName must not be null");

		verifyNoInteractions(kafkaTemplate);
	}

	@Test
	void publishDeletedRejectsNullInputs() {
		assertThatThrownBy(() -> publisher.publishDeleted(null, 3001L, "KB card"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("userId must not be null");
		assertThatThrownBy(() -> publisher.publishDeleted(101L, null, "KB card"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("cardId must not be null");
		assertThatThrownBy(() -> publisher.publishDeleted(101L, 3001L, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("cardName must not be null");

		verifyNoInteractions(kafkaTemplate);
	}

	@Test
	void publishDoesNotThrowWhenKafkaSendFutureFails() {
		when(kafkaTemplate.send(anyString(), anyString(), anyString()))
			.thenReturn(CompletableFuture.failedFuture(new RuntimeException("kafka failed")));

		assertThatCode(() -> publisher.publishRegistered(101L, 3001L, "KB국민 My WE:SH 카드"))
			.doesNotThrowAnyException();
	}

	@Test
	void publishDoesNotThrowWhenKafkaSendThrowsSynchronously() {
		when(kafkaTemplate.send(anyString(), anyString(), anyString()))
			.thenThrow(new RuntimeException("metadata unavailable"));

		assertThatCode(() -> publisher.publishRegistered(101L, 3001L, "KB국민 My WE:SH 카드"))
			.doesNotThrowAnyException();
	}
}
