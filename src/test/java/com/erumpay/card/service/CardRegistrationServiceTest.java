package com.erumpay.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.erumpay.card.domain.entity.CardProduct;
import com.erumpay.card.domain.enums.CardStatus;
import com.erumpay.card.dto.CardRegisterRequest;
import com.erumpay.card.exception.BillingKeyNotIntegratedException;
import com.erumpay.card.exception.BinMismatchException;
import com.erumpay.card.exception.CardProductNotFoundException;
import com.erumpay.card.exception.DuplicateCardRegistrationException;
import com.erumpay.card.repository.CardProductRepository;
import com.erumpay.card.repository.CardRegisteredRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CardRegistrationServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(
		Instant.parse("2026-05-20T00:00:00Z"),
		ZoneId.of("Asia/Seoul")
	);

	@Mock
	private CardProductRepository cardProductRepository;

	@Mock
	private CardRegisteredRepository cardRegisteredRepository;

	private CardRegistrationService cardRegistrationService;

	@BeforeEach
	void setUp() {
		cardRegistrationService = new CardRegistrationService(
			cardProductRepository,
			cardRegisteredRepository,
			FIXED_CLOCK
		);
	}

	@Test
	void registerFailsWhenMockBinDoesNotMatchCardNumber() {
		CardRegisterRequest request = request("800000", "8100001234567890");

		assertThatThrownBy(() -> cardRegistrationService.register(request))
			.isInstanceOf(BinMismatchException.class);

		verify(cardProductRepository, never()).findByMockBin(any());
		verify(cardRegisteredRepository, never()).existsByUserIdAndCardProductIdAndStatusIn(any(), any(), any());
	}

	@Test
	void registerFailsWhenCardProductDoesNotExist() {
		CardRegisterRequest request = request("800000", "8000001234567890");
		when(cardProductRepository.findByMockBin("800000")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> cardRegistrationService.register(request))
			.isInstanceOf(CardProductNotFoundException.class);

		verify(cardRegisteredRepository, never()).existsByUserIdAndCardProductIdAndStatusIn(any(), any(), any());
	}

	@Test
	void registerFailsWhenSameCardProductAlreadyRegistered() {
		CardRegisterRequest request = request("800000", "8000001234567890");
		CardProduct cardProduct = cardProduct(10L);

		when(cardProductRepository.findByMockBin("800000")).thenReturn(Optional.of(cardProduct));
		when(cardRegisteredRepository.existsByUserIdAndCardProductIdAndStatusIn(eq(1L), eq(10L), any()))
			.thenReturn(true);

		assertThatThrownBy(() -> cardRegistrationService.register(request))
			.isInstanceOf(DuplicateCardRegistrationException.class);
	}

	@Test
	void deletedCardProductRegistrationIsAllowedUntilBillingKeyIntegrationPoint() {
		CardRegisterRequest request = request("800000", "8000001234567890");
		CardProduct cardProduct = cardProduct(10L);

		when(cardProductRepository.findByMockBin("800000")).thenReturn(Optional.of(cardProduct));
		when(cardRegisteredRepository.existsByUserIdAndCardProductIdAndStatusIn(eq(1L), eq(10L), any()))
			.thenReturn(false);

		assertThatThrownBy(() -> cardRegistrationService.register(request))
			.isInstanceOf(BillingKeyNotIntegratedException.class);

		ArgumentCaptor<Collection<CardStatus>> statusesCaptor = ArgumentCaptor.forClass(Collection.class);
		verify(cardRegisteredRepository).existsByUserIdAndCardProductIdAndStatusIn(
			eq(1L),
			eq(10L),
			statusesCaptor.capture()
		);
		assertThat(statusesCaptor.getValue()).containsExactlyInAnyOrder(
			CardStatus.ACTIVE,
			CardStatus.PAUSED,
			CardStatus.EXPIRED
		);
	}

	@Test
	void registerFailsWithNotImplementedAfterValidationBeforeBillingKeyIntegration() {
		CardRegisterRequest request = request("800000", "8000001234567890");
		CardProduct cardProduct = cardProduct(10L);

		when(cardProductRepository.findByMockBin("800000")).thenReturn(Optional.of(cardProduct));
		when(cardRegisteredRepository.existsByUserIdAndCardProductIdAndStatusIn(eq(1L), eq(10L), any()))
			.thenReturn(false);

		assertThatThrownBy(() -> cardRegistrationService.register(request))
			.isInstanceOf(BillingKeyNotIntegratedException.class);
	}

	private CardRegisterRequest request(String mockBin, String cardNumber) {
		return new CardRegisterRequest(
			1L,
			mockBin,
			cardNumber,
			"202812",
			"123",
			"12",
			"생활비 카드",
			false
		);
	}

	private CardProduct cardProduct(Long cardProductId) {
		CardProduct cardProduct = mock(CardProduct.class);
		when(cardProduct.getCardProductId()).thenReturn(cardProductId);
		return cardProduct;
	}
}
