package com.erumpay.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.erumpay.card.client.AuthServiceClient;
import com.erumpay.card.client.CardSimulatorServiceClient;
import com.erumpay.card.domain.entity.CardPerformance;
import com.erumpay.card.domain.entity.CardProduct;
import com.erumpay.card.domain.entity.CardRegistered;
import com.erumpay.card.domain.enums.CardStatus;
import com.erumpay.card.dto.client.AuthUserInfoResponse;
import com.erumpay.card.dto.client.PerformanceInquireRequest;
import com.erumpay.card.dto.client.PerformanceInquireResponse;
import com.erumpay.card.repository.CardPerformanceRepository;
import com.erumpay.card.repository.CardProductRepository;
import com.erumpay.card.repository.CardRegisteredRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class CardPerformanceSyncServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(
		Instant.parse("2026-06-09T00:00:00Z"),
		ZoneId.of("Asia/Seoul")
	);

	@Mock
	private AuthServiceClient authServiceClient;

	@Mock
	private CardSimulatorServiceClient cardSimulatorServiceClient;

	@Mock
	private CardRegisteredRepository cardRegisteredRepository;

	@Mock
	private CardProductRepository cardProductRepository;

	@Mock
	private CardPerformanceRepository cardPerformanceRepository;

	private CardPerformanceSyncService cardPerformanceSyncService;

	@BeforeEach
	void setUp() {
		cardPerformanceSyncService = new CardPerformanceSyncService(
			authServiceClient,
			cardSimulatorServiceClient,
			cardRegisteredRepository,
			cardProductRepository,
			cardPerformanceRepository,
			transactionTemplate(),
			FIXED_CLOCK
		);
	}

	@Test
	void syncAfterRegistrationCreatesPreviousMonthPerformance() {
		CardProduct cardProduct = cardProduct("KB국민카드", "노리체크카드");
		when(cardSimulatorServiceClient.inquirePerformance(any()))
			.thenReturn(successResponse(123000L));
		when(cardPerformanceRepository.findByCardIdAndUserIdAndYearMonth(100L, 1L, "202605"))
			.thenReturn(Optional.empty());

		cardPerformanceSyncService.syncAfterRegistration(userInfo(), 100L, cardProduct);

		ArgumentCaptor<PerformanceInquireRequest> requestCaptor =
			ArgumentCaptor.forClass(PerformanceInquireRequest.class);
		verify(cardSimulatorServiceClient).inquirePerformance(requestCaptor.capture());
		PerformanceInquireRequest request = requestCaptor.getValue();
		assertThat(request.name()).isEqualTo("홍길동");
		assertThat(request.phoneNumber()).isEqualTo("01012345678");
		assertThat(request.cardCompany()).isEqualTo("KB국민카드");
		assertThat(request.productName()).isEqualTo("노리체크카드");
		assertThat(request.inquiryPeriod()).isEqualTo("202605");

		ArgumentCaptor<CardPerformance> performanceCaptor = ArgumentCaptor.forClass(CardPerformance.class);
		verify(cardPerformanceRepository).save(performanceCaptor.capture());
		CardPerformance performance = performanceCaptor.getValue();
		assertThat(performance.getUserId()).isEqualTo(1L);
		assertThat(performance.getCardId()).isEqualTo(100L);
		assertThat(performance.getYearMonth()).isEqualTo("202605");
		assertThat(performance.getAmount()).isEqualTo(123000L);
	}

	@Test
	void syncAfterRegistrationOverwritesExistingPerformance() {
		CardPerformance performance = CardPerformance.create(1L, 100L, "202605", 5000L);
		when(cardSimulatorServiceClient.inquirePerformance(any()))
			.thenReturn(successResponse(123000L));
		when(cardPerformanceRepository.findByCardIdAndUserIdAndYearMonth(100L, 1L, "202605"))
			.thenReturn(Optional.of(performance));

		cardPerformanceSyncService.syncAfterRegistration(userInfo(), 100L, cardProduct("KB국민카드", "노리체크카드"));

		assertThat(performance.getAmount()).isEqualTo(123000L);
		verify(cardPerformanceRepository, never()).save(any());
	}

	@Test
	void syncAfterRegistrationDoesNotThrowWhenSimulatorFails() {
		when(cardSimulatorServiceClient.inquirePerformance(any()))
			.thenThrow(new RuntimeException("simulator down"));

		assertThatCode(() ->
			cardPerformanceSyncService.syncAfterRegistration(userInfo(), 100L, cardProduct("KB국민카드", "노리체크카드"))
		).doesNotThrowAnyException();

		verify(cardPerformanceRepository, never()).save(any());
	}

	@Test
	void syncAfterRegistrationSkipsWhenUserInfoIsMissingRequiredFields() {
		AuthUserInfoResponse userInfo = new AuthUserInfoResponse(1L, "홍길동", "", "19900101", "ACTIVE");

		cardPerformanceSyncService.syncAfterRegistration(userInfo, 100L, cardProduct("KB국민카드", "노리체크카드"));

		verify(cardSimulatorServiceClient, never()).inquirePerformance(any());
		verify(cardPerformanceRepository, never()).save(any());
	}

	@Test
	void syncPreviousMonthForActiveCardsUsesOnlyActiveCardsFromRepository() {
		CardRegistered card = CardRegistered.registering(1L, 10L, "생활비", "202812");
		ReflectionTestUtils.setField(card, "cardId", 100L);
		when(cardRegisteredRepository.findByStatus(CardStatus.ACTIVE)).thenReturn(List.of(card));
		CardProduct product = cardProduct("KB국민카드", "노리체크카드");
		when(cardProductRepository.findById(10L)).thenReturn(Optional.of(product));
		when(authServiceClient.getUserInfo(1L)).thenReturn(userInfo());
		when(cardSimulatorServiceClient.inquirePerformance(any()))
			.thenReturn(successResponse(10000L));
		when(cardPerformanceRepository.findByCardIdAndUserIdAndYearMonth(100L, 1L, "202605"))
			.thenReturn(Optional.empty());

		cardPerformanceSyncService.syncPreviousMonthForActiveCards();

		verify(cardRegisteredRepository).findByStatus(CardStatus.ACTIVE);
		verify(cardProductRepository).findById(10L);
		verify(authServiceClient).getUserInfo(1L);
		verify(cardPerformanceRepository).save(any(CardPerformance.class));
	}

	private AuthUserInfoResponse userInfo() {
		return new AuthUserInfoResponse(1L, "홍길동", "01012345678", "19900101", "ACTIVE");
	}

	private CardProduct cardProduct(String cardCompany, String cardName) {
		CardProduct cardProduct = mock(CardProduct.class);
		lenient().when(cardProduct.getCardCompany()).thenReturn(cardCompany);
		lenient().when(cardProduct.getCardName()).thenReturn(cardName);
		return cardProduct;
	}

	private PerformanceInquireResponse successResponse(Long amount) {
		return new PerformanceInquireResponse(
			"KB국민카드",
			"노리체크카드",
			"202605",
			amount,
			200,
			"SIM-CARD-200",
			"CARD_SUCCESS",
			"정상 처리되었습니다."
		);
	}

	private TransactionTemplate transactionTemplate() {
		return new TransactionTemplate(new PlatformTransactionManager() {
			@Override
			public TransactionStatus getTransaction(TransactionDefinition definition) {
				return new SimpleTransactionStatus();
			}

			@Override
			public void commit(TransactionStatus status) {
			}

			@Override
			public void rollback(TransactionStatus status) {
			}
		});
	}
}
