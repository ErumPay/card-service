package com.erumpay.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.erumpay.card.domain.entity.CardProduct;
import com.erumpay.card.domain.entity.CardRegistered;
import com.erumpay.card.domain.enums.CardStatus;
import com.erumpay.card.dto.CardAliasUpdateRequest;
import com.erumpay.card.dto.CardResponse;
import com.erumpay.card.dto.PaymentAvailabilityResponse;
import com.erumpay.card.exception.CardNotActiveException;
import com.erumpay.card.exception.CardNotFoundException;
import com.erumpay.card.repository.CardProductRepository;
import com.erumpay.card.repository.CardRegisteredRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CardManagementServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(
		Instant.parse("2026-05-20T00:00:00Z"),
		ZoneId.of("Asia/Seoul")
	);

	@Mock
	private CardRegisteredRepository cardRegisteredRepository;

	@Mock
	private CardProductRepository cardProductRepository;

	private CardManagementService cardManagementService;

	@BeforeEach
	void setUp() {
		cardManagementService = new CardManagementService(
			cardRegisteredRepository,
			cardProductRepository,
			FIXED_CLOCK
		);
	}

	@Test
	void getCardsReturnsRegisteredCardResponses() {
		CardRegistered card = card(10L, 1L, 100L, CardStatus.ACTIVE, true, "billing-key");
		CardProduct product = product(100L, "롯데카드", "LOCA 365 카드");

		when(cardRegisteredRepository.findByUserIdAndStatusInOrderByDefaultCardDescCreatedAtDesc(eq(1L), any()))
			.thenReturn(List.of(card));
		when(cardProductRepository.findAllById(any())).thenReturn(List.of(product));

		List<CardResponse> responses = cardManagementService.getCards(1L);

		assertThat(responses).hasSize(1);
		assertThat(responses.getFirst().getCardId()).isEqualTo(10L);
		assertThat(responses.getFirst().getCardCompany()).isEqualTo("롯데카드");
		assertThat(responses.getFirst().getCardName()).isEqualTo("LOCA 365 카드");
		assertThat(responses.getFirst().getIsDefault()).isTrue();
	}

	@Test
	void getCardFailsWhenCardDoesNotBelongToUser() {
		when(cardRegisteredRepository.findByCardIdAndUserIdAndStatusIn(eq(10L), eq(1L), any()))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> cardManagementService.getCard(1L, 10L))
			.isInstanceOf(CardNotFoundException.class);
	}

	@Test
	void updateAliasNormalizesBlankAliasToNull() {
		CardRegistered card = card(10L, 1L, 100L, CardStatus.ACTIVE, false, "billing-key");
		when(cardRegisteredRepository.findByCardIdAndUserIdAndStatusIn(eq(10L), eq(1L), any()))
			.thenReturn(Optional.of(card));

		cardManagementService.updateAlias(1L, 10L, new CardAliasUpdateRequest("   "));

		verify(card).updateAlias(null);
	}

	@Test
	void updateAliasNormalizesTrailingSpaceBeforeApplying() {
		CardRegistered card = card(10L, 1L, 100L, CardStatus.ACTIVE, false, "billing-key");
		when(cardRegisteredRepository.findByCardIdAndUserIdAndStatusIn(eq(10L), eq(1L), any()))
			.thenReturn(Optional.of(card));

		CardAliasUpdateRequest request = new CardAliasUpdateRequest();
		request.setCardAlias("1234567890 ");

		cardManagementService.updateAlias(1L, 10L, request);

		verify(card).updateAlias("1234567890");
	}

	@Test
	void setDefaultFailsWhenTargetCardIsNotActive() {
		CardRegistered card = card(10L, 1L, 100L, CardStatus.PAUSED, false, "billing-key");
		when(cardRegisteredRepository.findByCardIdAndUserIdAndStatusIn(eq(10L), eq(1L), any()))
			.thenReturn(Optional.of(card));

		assertThatThrownBy(() -> cardManagementService.setDefault(1L, 10L))
			.isInstanceOf(CardNotActiveException.class);

		verify(cardRegisteredRepository, never()).findByUserIdAndDefaultCardTrueAndStatus(any(), any());
	}

	@Test
	void setDefaultUnsetsCurrentDefaultBeforeMarkingTargetDefault() {
		CardRegistered currentDefault = card(9L, 1L, 99L, CardStatus.ACTIVE, true, "billing-key");
		CardRegistered target = card(10L, 1L, 100L, CardStatus.ACTIVE, false, "billing-key");

		when(cardRegisteredRepository.findByCardIdAndUserIdAndStatusIn(eq(10L), eq(1L), any()))
			.thenReturn(Optional.of(target));
		when(cardRegisteredRepository.findByUserIdAndDefaultCardTrueAndStatus(1L, CardStatus.ACTIVE))
			.thenReturn(Optional.of(currentDefault));

		cardManagementService.setDefault(1L, 10L);

		InOrder inOrder = inOrder(currentDefault, target);
		inOrder.verify(currentDefault).unsetDefault();
		inOrder.verify(target).markDefault();
	}

	@Test
	void deleteDefaultCardAssignsReplacementAfterSoftDelete() {
		CardRegistered target = card(10L, 1L, 100L, CardStatus.ACTIVE, true, "billing-key");
		CardRegistered replacement = card(11L, 1L, 101L, CardStatus.ACTIVE, false, "billing-key");

		when(cardRegisteredRepository.findByCardIdAndUserId(10L, 1L)).thenReturn(Optional.of(target));
		when(cardRegisteredRepository.findFirstByUserIdAndStatusAndCardIdNotOrderByCreatedAtAscCardIdAsc(
			1L,
			CardStatus.ACTIVE,
			10L
		)).thenReturn(Optional.of(replacement));

		cardManagementService.deleteCard(1L, 10L);

		InOrder inOrder = inOrder(target, replacement);
		inOrder.verify(target).delete(any(LocalDateTime.class));
		inOrder.verify(replacement).markDefault();
	}

	@Test
	void deleteAlreadyDeletedCardDoesNothing() {
		CardRegistered target = card(10L, 1L, 100L, CardStatus.DELETED, false, "billing-key");
		when(cardRegisteredRepository.findByCardIdAndUserId(10L, 1L)).thenReturn(Optional.of(target));

		cardManagementService.deleteCard(1L, 10L);

		verify(target, never()).delete(any());
		verify(cardRegisteredRepository, never())
			.findFirstByUserIdAndStatusAndCardIdNotOrderByCreatedAtAscCardIdAsc(any(), any(), any());
	}

	@Test
	void deleteRegisteringCardFailsAsNotFound() {
		CardRegistered target = card(10L, 1L, 100L, CardStatus.REGISTERING, false, null);
		when(cardRegisteredRepository.findByCardIdAndUserId(10L, 1L)).thenReturn(Optional.of(target));

		assertThatThrownBy(() -> cardManagementService.deleteCard(1L, 10L))
			.isInstanceOf(CardNotFoundException.class);

		verify(target, never()).delete(any());
	}

	@Test
	void checkUserPaymentAvailabilityReturnsCardNotFoundWhenUserHasNoCards() {
		when(cardRegisteredRepository.existsByUserIdAndStatusIn(eq(1L), any())).thenReturn(false);

		PaymentAvailabilityResponse response = cardManagementService.checkUserPaymentAvailability(1L);

		assertThat(response.isAvailable()).isFalse();
		assertThat(response.getReason()).isEqualTo("CARD_NOT_FOUND");
	}

	@Test
	void checkUserPaymentAvailabilityReturnsCardNotActiveWhenUserHasNoActiveCards() {
		when(cardRegisteredRepository.existsByUserIdAndStatusIn(eq(1L), any())).thenReturn(true);
		when(cardRegisteredRepository.existsByUserIdAndStatus(1L, CardStatus.ACTIVE)).thenReturn(false);

		PaymentAvailabilityResponse response = cardManagementService.checkUserPaymentAvailability(1L);

		assertThat(response.isAvailable()).isFalse();
		assertThat(response.getReason()).isEqualTo("CARD_NOT_ACTIVE");
	}

	@Test
	void checkUserPaymentAvailabilityReturnsBillingKeyNotFoundWhenActiveCardHasNoBillingKey() {
		when(cardRegisteredRepository.existsByUserIdAndStatusIn(eq(1L), any())).thenReturn(true);
		when(cardRegisteredRepository.existsByUserIdAndStatus(1L, CardStatus.ACTIVE)).thenReturn(true);
		when(cardRegisteredRepository.existsPaymentAvailableCard(1L, CardStatus.ACTIVE)).thenReturn(false);

		PaymentAvailabilityResponse response = cardManagementService.checkUserPaymentAvailability(1L);

		assertThat(response.isAvailable()).isFalse();
		assertThat(response.getReason()).isEqualTo("BILLING_KEY_NOT_FOUND");
	}

	@Test
	void checkUserPaymentAvailabilityReturnsAvailableWhenActiveCardHasBillingKey() {
		when(cardRegisteredRepository.existsByUserIdAndStatusIn(eq(1L), any())).thenReturn(true);
		when(cardRegisteredRepository.existsByUserIdAndStatus(1L, CardStatus.ACTIVE)).thenReturn(true);
		when(cardRegisteredRepository.existsPaymentAvailableCard(1L, CardStatus.ACTIVE)).thenReturn(true);

		PaymentAvailabilityResponse response = cardManagementService.checkUserPaymentAvailability(1L);

		assertThat(response.isAvailable()).isTrue();
		assertThat(response.getReason()).isNull();
	}

	@Test
	void checkCardPaymentAvailabilityFailsWhenCardDoesNotBelongToUser() {
		when(cardRegisteredRepository.findByCardIdAndUserIdAndStatusIn(eq(10L), eq(1L), any()))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> cardManagementService.checkCardPaymentAvailability(1L, 10L))
			.isInstanceOf(CardNotFoundException.class);
	}

	@Test
	void checkCardPaymentAvailabilityReturnsCardNotActiveWhenCardIsNotActive() {
		CardRegistered card = card(10L, 1L, 100L, CardStatus.EXPIRED, false, "billing-key");
		when(cardRegisteredRepository.findByCardIdAndUserIdAndStatusIn(eq(10L), eq(1L), any()))
			.thenReturn(Optional.of(card));

		PaymentAvailabilityResponse response = cardManagementService.checkCardPaymentAvailability(1L, 10L);

		assertThat(response.isAvailable()).isFalse();
		assertThat(response.getReason()).isEqualTo("CARD_NOT_ACTIVE");
	}

	@Test
	void checkCardPaymentAvailabilityReturnsBillingKeyNotFoundWhenCardHasNoBillingKey() {
		CardRegistered card = card(10L, 1L, 100L, CardStatus.ACTIVE, false, null);
		when(cardRegisteredRepository.findByCardIdAndUserIdAndStatusIn(eq(10L), eq(1L), any()))
			.thenReturn(Optional.of(card));

		PaymentAvailabilityResponse response = cardManagementService.checkCardPaymentAvailability(1L, 10L);

		assertThat(response.isAvailable()).isFalse();
		assertThat(response.getReason()).isEqualTo("BILLING_KEY_NOT_FOUND");
	}

	@Test
	void checkCardPaymentAvailabilityReturnsAvailableWhenCardIsActiveAndHasBillingKey() {
		CardRegistered card = card(10L, 1L, 100L, CardStatus.ACTIVE, false, "billing-key");
		when(cardRegisteredRepository.findByCardIdAndUserIdAndStatusIn(eq(10L), eq(1L), any()))
			.thenReturn(Optional.of(card));

		PaymentAvailabilityResponse response = cardManagementService.checkCardPaymentAvailability(1L, 10L);

		assertThat(response.isAvailable()).isTrue();
		assertThat(response.getReason()).isNull();
	}

	private CardRegistered card(
		Long cardId,
		Long userId,
		Long cardProductId,
		CardStatus status,
		boolean defaultCard,
		String billingKey
	) {
		CardRegistered card = mock(CardRegistered.class);
		lenient().when(card.getCardId()).thenReturn(cardId);
		lenient().when(card.getUserId()).thenReturn(userId);
		lenient().when(card.getCardProductId()).thenReturn(cardProductId);
		lenient().when(card.getMaskedNumber()).thenReturn("8000-****-****-1234");
		lenient().when(card.getCardAlias()).thenReturn("생활비");
		lenient().when(card.getExpiryYm()).thenReturn("202812");
		lenient().when(card.isDefaultCard()).thenReturn(defaultCard);
		lenient().when(card.getStatus()).thenReturn(status);
		lenient().when(card.isDeleted()).thenReturn(status == CardStatus.DELETED);
		lenient().when(card.isRegistering()).thenReturn(status == CardStatus.REGISTERING);
		lenient().when(card.isActive()).thenReturn(status == CardStatus.ACTIVE);
		lenient().when(card.hasBillingKey()).thenReturn(billingKey != null && !billingKey.isBlank());
		return card;
	}

	private CardProduct product(Long cardProductId, String cardCompany, String cardName) {
		CardProduct product = mock(CardProduct.class);
		lenient().when(product.getCardProductId()).thenReturn(cardProductId);
		lenient().when(product.getCardCompany()).thenReturn(cardCompany);
		lenient().when(product.getCardName()).thenReturn(cardName);
		return product;
	}
}
