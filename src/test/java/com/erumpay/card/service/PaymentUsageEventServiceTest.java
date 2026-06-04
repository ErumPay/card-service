package com.erumpay.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.erumpay.card.domain.entity.CardBenefitUsage;
import com.erumpay.card.domain.entity.CardPerformance;
import com.erumpay.card.domain.entity.CardRegistered;
import com.erumpay.card.domain.enums.CardBenefitUsageStatus;
import com.erumpay.card.domain.enums.CardStatus;
import com.erumpay.card.domain.enums.PaymentUsageEventType;
import com.erumpay.card.dto.PaymentUsageEventRequest;
import com.erumpay.card.dto.PaymentUsageEventRequest.AppliedBenefitRequest;
import com.erumpay.card.dto.PaymentUsageEventRequest.PaymentUsageCardRequest;
import com.erumpay.card.dto.PaymentUsageEventResponse;
import com.erumpay.card.exception.CardNotActiveException;
import com.erumpay.card.exception.InvalidPaymentUsageEventRequestException;
import com.erumpay.card.repository.CardBenefitRepository;
import com.erumpay.card.repository.CardBenefitTierRepository;
import com.erumpay.card.repository.CardBenefitUsageRepository;
import com.erumpay.card.repository.CardPerformanceRepository;
import com.erumpay.card.repository.CardRegisteredRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentUsageEventServiceTest {

	@Mock
	private CardRegisteredRepository cardRegisteredRepository;

	@Mock
	private CardPerformanceRepository cardPerformanceRepository;

	@Mock
	private CardBenefitRepository cardBenefitRepository;

	@Mock
	private CardBenefitTierRepository cardBenefitTierRepository;

	@Mock
	private CardBenefitUsageRepository cardBenefitUsageRepository;

	private PaymentUsageEventService paymentUsageEventService;

	@BeforeEach
	void setUp() {
		paymentUsageEventService = new PaymentUsageEventService(
			cardRegisteredRepository,
			cardPerformanceRepository,
			cardBenefitRepository,
			cardBenefitTierRepository,
			cardBenefitUsageRepository
		);
	}

	@Test
	void approvedCreatesPerformanceAndBenefitUsage() {
		CardRegistered card = card(10L, 100L, CardStatus.ACTIVE);
		PaymentUsageEventRequest request = request(
			1L,
			PaymentUsageEventType.APPROVED,
			LocalDateTime.of(2026, 6, 2, 20, 10),
			cardRequest(1000L, 10L, 12000L, null, benefit(33L, 91L, 1200L))
		);
		when(cardRegisteredRepository.findByUserIdAndCardIdIn(2L, List.of(10L))).thenReturn(List.of(card));
		when(cardBenefitRepository.existsByBenefitIdAndCardProductId(33L, 100L)).thenReturn(true);
		when(cardBenefitTierRepository.existsByTierIdAndBenefitId(91L, 33L)).thenReturn(true);
		when(cardPerformanceRepository.findByCardIdAndUserIdAndYearMonth(10L, 2L, "202606"))
			.thenReturn(Optional.empty());

		PaymentUsageEventResponse response = paymentUsageEventService.apply(2L, request);

		assertThat(response.getPaymentId()).isEqualTo(1L);
		assertThat(response.getEventType()).isEqualTo(PaymentUsageEventType.APPROVED);
		assertThat(response.isApplied()).isTrue();
		assertThat(response.getAppliedCardCount()).isEqualTo(1);

		ArgumentCaptor<CardPerformance> performanceCaptor = ArgumentCaptor.forClass(CardPerformance.class);
		verify(cardPerformanceRepository).save(performanceCaptor.capture());
		CardPerformance performance = performanceCaptor.getValue();
		assertThat(performance.getUserId()).isEqualTo(2L);
		assertThat(performance.getCardId()).isEqualTo(10L);
		assertThat(performance.getYearMonth()).isEqualTo("202606");
		assertThat(performance.getAmount()).isEqualTo(12000L);

		ArgumentCaptor<CardBenefitUsage> usageCaptor = ArgumentCaptor.forClass(CardBenefitUsage.class);
		verify(cardBenefitUsageRepository).save(usageCaptor.capture());
		CardBenefitUsage usage = usageCaptor.getValue();
		assertThat(usage.getPaymentId()).isEqualTo(1L);
		assertThat(usage.getUserId()).isEqualTo(2L);
		assertThat(usage.getCardId()).isEqualTo(10L);
		assertThat(usage.getBenefitId()).isEqualTo(33L);
		assertThat(usage.getTierId()).isEqualTo(91L);
		assertThat(usage.getApprovedAmount()).isEqualTo(12000);
		assertThat(usage.getBenefitAmount()).isEqualTo(1200);
		assertThat(usage.getApprovedAt()).isEqualTo(LocalDateTime.of(2026, 6, 2, 20, 10));
		assertThat(usage.getStatus()).isEqualTo(CardBenefitUsageStatus.APPROVED);
	}

	@Test
	void approvedWithoutBenefitOnlyIncreasesPerformance() {
		CardRegistered card = card(10L, 100L, CardStatus.ACTIVE);
		CardPerformance performance = CardPerformance.create(2L, 10L, "202606", 3000L);
		PaymentUsageEventRequest request = request(
			1L,
			PaymentUsageEventType.APPROVED,
			LocalDateTime.of(2026, 6, 2, 20, 10),
			cardRequest(1000L, 10L, 12000L, null, null)
		);
		when(cardRegisteredRepository.findByUserIdAndCardIdIn(2L, List.of(10L))).thenReturn(List.of(card));
		when(cardPerformanceRepository.findByCardIdAndUserIdAndYearMonth(10L, 2L, "202606"))
			.thenReturn(Optional.of(performance));

		paymentUsageEventService.apply(2L, request);

		assertThat(performance.getAmount()).isEqualTo(15000L);
		verify(cardBenefitUsageRepository, never()).save(any());
	}

	@Test
	void canceledUsesApprovedMonthAndCancelsBenefitUsage() {
		CardRegistered card = card(10L, 100L, CardStatus.PAUSED);
		CardPerformance performance = CardPerformance.create(2L, 10L, "202605", 50000L);
		CardBenefitUsage usage = CardBenefitUsage.approved(
			1L,
			2L,
			10L,
			33L,
			91L,
			12000,
			1200,
			LocalDateTime.of(2026, 5, 31, 23, 30)
		);
		PaymentUsageEventRequest request = request(
			1L,
			PaymentUsageEventType.CANCELED,
			LocalDateTime.of(2026, 6, 2, 20, 10),
			cardRequest(
				1000L,
				10L,
				12000L,
				LocalDateTime.of(2026, 5, 31, 23, 30),
				benefit(33L, 91L, 1200L)
			)
		);
		when(cardRegisteredRepository.findByUserIdAndCardIdIn(2L, List.of(10L))).thenReturn(List.of(card));
		when(cardPerformanceRepository.findByCardIdAndUserIdAndYearMonth(10L, 2L, "202605"))
			.thenReturn(Optional.of(performance));
		when(cardBenefitUsageRepository.findByPaymentIdAndCardId(1L, 10L)).thenReturn(List.of(usage));

		paymentUsageEventService.apply(2L, request);

		assertThat(performance.getAmount()).isEqualTo(38000L);
		assertThat(usage.getStatus()).isEqualTo(CardBenefitUsageStatus.CANCELED);
		assertThat(usage.getCanceledAt()).isEqualTo(LocalDateTime.of(2026, 6, 2, 20, 10));
	}

	@Test
	void canceledWithoutBenefitOnlyDecreasesPerformance() {
		CardRegistered card = card(10L, 100L, CardStatus.EXPIRED);
		CardPerformance performance = CardPerformance.create(2L, 10L, "202605", 3000L);
		PaymentUsageEventRequest request = request(
			1L,
			PaymentUsageEventType.CANCELED,
			LocalDateTime.of(2026, 6, 2, 20, 10),
			cardRequest(
				1000L,
				10L,
				12000L,
				LocalDateTime.of(2026, 5, 31, 23, 30),
				null
			)
		);
		when(cardRegisteredRepository.findByUserIdAndCardIdIn(2L, List.of(10L))).thenReturn(List.of(card));
		when(cardPerformanceRepository.findByCardIdAndUserIdAndYearMonth(10L, 2L, "202605"))
			.thenReturn(Optional.of(performance));
		when(cardBenefitUsageRepository.findByPaymentIdAndCardId(1L, 10L)).thenReturn(List.of());

		paymentUsageEventService.apply(2L, request);

		assertThat(performance.getAmount()).isZero();
	}

	@Test
	void canceledWithoutAppliedBenefitCancelsExistingBenefitUsage() {
		CardRegistered card = card(10L, 100L, CardStatus.PAUSED);
		CardPerformance performance = CardPerformance.create(2L, 10L, "202605", 50000L);
		CardBenefitUsage usage = CardBenefitUsage.approved(
			1L,
			2L,
			10L,
			33L,
			91L,
			12000,
			1200,
			LocalDateTime.of(2026, 5, 31, 23, 30)
		);
		PaymentUsageEventRequest request = request(
			1L,
			PaymentUsageEventType.CANCELED,
			LocalDateTime.of(2026, 6, 2, 20, 10),
			cardRequest(
				1000L,
				10L,
				12000L,
				LocalDateTime.of(2026, 5, 31, 23, 30),
				null
			)
		);
		when(cardRegisteredRepository.findByUserIdAndCardIdIn(2L, List.of(10L))).thenReturn(List.of(card));
		when(cardBenefitUsageRepository.findByPaymentIdAndCardId(1L, 10L)).thenReturn(List.of(usage));
		when(cardPerformanceRepository.findByCardIdAndUserIdAndYearMonth(10L, 2L, "202605"))
			.thenReturn(Optional.of(performance));

		paymentUsageEventService.apply(2L, request);

		assertThat(usage.getStatus()).isEqualTo(CardBenefitUsageStatus.CANCELED);
		assertThat(performance.getAmount()).isEqualTo(38000L);
	}

	@Test
	void duplicateCanceledBenefitUsageDoesNotDecreasePerformanceAgain() {
		CardRegistered card = card(10L, 100L, CardStatus.PAUSED);
		CardPerformance performance = CardPerformance.create(2L, 10L, "202605", 38000L);
		CardBenefitUsage usage = CardBenefitUsage.approved(
			1L,
			2L,
			10L,
			33L,
			91L,
			12000,
			1200,
			LocalDateTime.of(2026, 5, 31, 23, 30)
		);
		usage.cancel(LocalDateTime.of(2026, 6, 2, 20, 10));
		PaymentUsageEventRequest request = request(
			1L,
			PaymentUsageEventType.CANCELED,
			LocalDateTime.of(2026, 6, 2, 20, 15),
			cardRequest(
				1000L,
				10L,
				12000L,
				LocalDateTime.of(2026, 5, 31, 23, 30),
				null
			)
		);
		when(cardRegisteredRepository.findByUserIdAndCardIdIn(2L, List.of(10L))).thenReturn(List.of(card));
		when(cardBenefitUsageRepository.findByPaymentIdAndCardId(1L, 10L)).thenReturn(List.of(usage));

		paymentUsageEventService.apply(2L, request);

		assertThat(performance.getAmount()).isEqualTo(38000L);
		verify(cardPerformanceRepository, never()).findByCardIdAndUserIdAndYearMonth(any(), any(), any());
	}

	@Test
	void approvedRejectsInactiveCard() {
		CardRegistered card = card(10L, 100L, CardStatus.PAUSED);
		PaymentUsageEventRequest request = request(
			1L,
			PaymentUsageEventType.APPROVED,
			LocalDateTime.of(2026, 6, 2, 20, 10),
			cardRequest(1000L, 10L, 12000L, null, null)
		);
		when(cardRegisteredRepository.findByUserIdAndCardIdIn(2L, List.of(10L))).thenReturn(List.of(card));

		assertThatThrownBy(() -> paymentUsageEventService.apply(2L, request))
			.isInstanceOf(CardNotActiveException.class);

		verify(cardPerformanceRepository, never()).save(any());
	}

	@Test
	void canceledRequiresApprovedAt() {
		PaymentUsageEventRequest request = request(
			1L,
			PaymentUsageEventType.CANCELED,
			LocalDateTime.of(2026, 6, 2, 20, 10),
			cardRequest(1000L, 10L, 12000L, null, null)
		);

		assertThatThrownBy(() -> paymentUsageEventService.apply(2L, request))
			.isInstanceOf(InvalidPaymentUsageEventRequestException.class)
			.hasMessage("approvedAt is required for canceled event");

		verify(cardRegisteredRepository, never()).findByUserIdAndCardIdIn(any(), any());
	}

	private PaymentUsageEventRequest request(
		Long paymentId,
		PaymentUsageEventType eventType,
		LocalDateTime occurredAt,
		PaymentUsageCardRequest card
	) {
		return new PaymentUsageEventRequest(paymentId, eventType, occurredAt, List.of(card));
	}

	private PaymentUsageCardRequest cardRequest(
		Long paymentCardId,
		Long cardId,
		Long approvedAmount,
		LocalDateTime approvedAt,
		AppliedBenefitRequest benefit
	) {
		return new PaymentUsageCardRequest(paymentCardId, cardId, approvedAmount, approvedAt, benefit);
	}

	private AppliedBenefitRequest benefit(Long benefitId, Long tierId, Long benefitAmount) {
		return new AppliedBenefitRequest(benefitId, tierId, benefitAmount);
	}

	private CardRegistered card(Long cardId, Long cardProductId, CardStatus status) {
		CardRegistered card = org.mockito.Mockito.mock(CardRegistered.class);
		lenient().when(card.getCardId()).thenReturn(cardId);
		lenient().when(card.getCardProductId()).thenReturn(cardProductId);
		lenient().when(card.getStatus()).thenReturn(status);
		lenient().when(card.isActive()).thenReturn(status == CardStatus.ACTIVE);
		return card;
	}
}
