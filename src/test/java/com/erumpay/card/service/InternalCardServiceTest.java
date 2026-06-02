package com.erumpay.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.erumpay.card.client.BillingKeyServiceClient;
import com.erumpay.card.domain.entity.CardBenefit;
import com.erumpay.card.domain.entity.CardBenefitBrand;
import com.erumpay.card.domain.entity.CardBenefitTier;
import com.erumpay.card.domain.entity.CardBenefitUsage;
import com.erumpay.card.domain.entity.CardPerformance;
import com.erumpay.card.domain.entity.CardProduct;
import com.erumpay.card.domain.entity.CardRegistered;
import com.erumpay.card.domain.enums.CardBenefitUsageStatus;
import com.erumpay.card.domain.enums.BenefitType;
import com.erumpay.card.domain.enums.CardStatus;
import com.erumpay.card.domain.enums.DayCondition;
import com.erumpay.card.domain.enums.ServiceCategory;
import com.erumpay.card.dto.InternalBillingKeyResponse;
import com.erumpay.card.dto.InternalDeactivateCardsResponse;
import com.erumpay.card.dto.InternalDefaultCardResponse;
import com.erumpay.card.dto.InternalRecommendationSourceResponse;
import com.erumpay.card.dto.client.BillingKeyDeleteRequest;
import com.erumpay.card.dto.client.BillingKeyDeleteResponse;
import com.erumpay.card.exception.BillingKeyDeactivationFailedException;
import com.erumpay.card.exception.BillingKeyNotFoundException;
import com.erumpay.card.exception.BillingKeyServiceUnavailableException;
import com.erumpay.card.exception.CardNotActiveException;
import com.erumpay.card.exception.CardNotFoundException;
import com.erumpay.card.exception.InvalidYearMonthException;
import com.erumpay.card.repository.CardBenefitBrandRepository;
import com.erumpay.card.repository.CardBenefitRepository;
import com.erumpay.card.repository.CardBenefitTierRepository;
import com.erumpay.card.repository.CardBenefitUsageRepository;
import com.erumpay.card.repository.CardPerformanceRepository;
import com.erumpay.card.repository.CardProductRepository;
import com.erumpay.card.repository.CardRegisteredRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class InternalCardServiceTest {

	private static final Clock FIXED_CLOCK = Clock.fixed(
		Instant.parse("2026-05-20T00:00:00Z"),
		ZoneId.of("Asia/Seoul")
	);

	@Mock
	private CardRegisteredRepository cardRegisteredRepository;

	@Mock
	private CardProductRepository cardProductRepository;

	@Mock
	private CardPerformanceRepository cardPerformanceRepository;

	@Mock
	private CardBenefitRepository cardBenefitRepository;

	@Mock
	private CardBenefitBrandRepository cardBenefitBrandRepository;

	@Mock
	private CardBenefitTierRepository cardBenefitTierRepository;

	@Mock
	private CardBenefitUsageRepository cardBenefitUsageRepository;

	@Mock
	private BillingKeyServiceClient billingKeyServiceClient;

	private BillingKeyCryptoService billingKeyCryptoService;

	private InternalCardService internalCardService;

	@BeforeEach
	void setUp() {
		billingKeyCryptoService = billingKeyCryptoService();
		internalCardService = new InternalCardService(
			cardRegisteredRepository,
			cardProductRepository,
			cardPerformanceRepository,
			cardBenefitRepository,
			cardBenefitBrandRepository,
			cardBenefitTierRepository,
			cardBenefitUsageRepository,
			billingKeyServiceClient,
			billingKeyCryptoService,
			FIXED_CLOCK,
			new YearMonthValidator(),
			transactionTemplate()
		);
	}

	@Test
	void getBillingKeyReturnsBillingKeyForActiveOwnedCard() {
		String encryptedBillingKey = billingKeyCryptoService.encrypt("billing-key");
		CardRegistered card = card(10L, 1L, 100L, CardStatus.ACTIVE, false, encryptedBillingKey);
		when(cardRegisteredRepository.findByCardIdAndUserIdAndStatusNot(10L, 1L, CardStatus.DELETED))
			.thenReturn(Optional.of(card));

		InternalBillingKeyResponse response = internalCardService.getBillingKey(1L, 10L);

		assertThat(response.getCardId()).isEqualTo(10L);
		assertThat(response.getUserId()).isEqualTo(1L);
		assertThat(response.getBillingKey()).isEqualTo("billing-key");
		assertThat(response.toString()).doesNotContain("billing-key");
	}

	@Test
	void getBillingKeyReturnsLegacyPlainBillingKey() {
		CardRegistered card = card(10L, 1L, 100L, CardStatus.ACTIVE, false, "legacy-billing-key");
		when(cardRegisteredRepository.findByCardIdAndUserIdAndStatusNot(10L, 1L, CardStatus.DELETED))
			.thenReturn(Optional.of(card));

		InternalBillingKeyResponse response = internalCardService.getBillingKey(1L, 10L);

		assertThat(response.getBillingKey()).isEqualTo("legacy-billing-key");
	}

	@Test
	void getBillingKeyFailsWhenCardDoesNotBelongToUser() {
		when(cardRegisteredRepository.findByCardIdAndUserIdAndStatusNot(10L, 1L, CardStatus.DELETED))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> internalCardService.getBillingKey(1L, 10L))
			.isInstanceOf(CardNotFoundException.class);
	}

	@Test
	void getBillingKeyFailsWhenCardIsNotActive() {
		CardRegistered card = card(10L, 1L, 100L, CardStatus.PAUSED, false, "encrypted-key");
		when(cardRegisteredRepository.findByCardIdAndUserIdAndStatusNot(10L, 1L, CardStatus.DELETED))
			.thenReturn(Optional.of(card));

		assertThatThrownBy(() -> internalCardService.getBillingKey(1L, 10L))
			.isInstanceOf(CardNotActiveException.class);
	}

	@Test
	void getBillingKeyFailsAsNotFoundWhenCardIsRegistering() {
		CardRegistered card = card(10L, 1L, 100L, CardStatus.REGISTERING, false, null);
		when(cardRegisteredRepository.findByCardIdAndUserIdAndStatusNot(10L, 1L, CardStatus.DELETED))
			.thenReturn(Optional.of(card));

		assertThatThrownBy(() -> internalCardService.getBillingKey(1L, 10L))
			.isInstanceOf(CardNotFoundException.class);
	}

	@Test
	void getBillingKeyFailsWhenBillingKeyDoesNotExist() {
		CardRegistered card = card(10L, 1L, 100L, CardStatus.ACTIVE, false, "   ");
		when(cardRegisteredRepository.findByCardIdAndUserIdAndStatusNot(10L, 1L, CardStatus.DELETED))
			.thenReturn(Optional.of(card));

		assertThatThrownBy(() -> internalCardService.getBillingKey(1L, 10L))
			.isInstanceOf(BillingKeyNotFoundException.class);
	}

	@Test
	void getDefaultCardReturnsActiveDefaultCard() {
		CardRegistered card = card(10L, 1L, 100L, CardStatus.ACTIVE, true, "encrypted-key");
		CardProduct product = product(100L, "롯데카드", "LOCA 365 카드");

		when(cardRegisteredRepository.findByUserIdAndDefaultCardTrueAndStatus(1L, CardStatus.ACTIVE))
			.thenReturn(Optional.of(card));
		when(cardProductRepository.findById(100L)).thenReturn(Optional.of(product));

		InternalDefaultCardResponse response = internalCardService.getDefaultCard(1L);

		assertThat(response.getCardId()).isEqualTo(10L);
		assertThat(response.getCardCompany()).isEqualTo("롯데카드");
		assertThat(response.getCardName()).isEqualTo("LOCA 365 카드");
	}

	@Test
	void getDefaultCardFailsWhenDefaultCardDoesNotExist() {
		when(cardRegisteredRepository.findByUserIdAndDefaultCardTrueAndStatus(1L, CardStatus.ACTIVE))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> internalCardService.getDefaultCard(1L))
			.isInstanceOf(CardNotFoundException.class);
	}

	@Test
	void getRecommendationSourceReturnsCardsWithPerformanceAndBenefits() {
		CardRegistered card = card(10L, 1L, 100L, CardStatus.ACTIVE, true, "encrypted-key");
		CardProduct product = product(100L, "롯데카드", "LOCA 365 카드");
		CardPerformance performance = performance(10L, 350000L);
		CardBenefit benefit = benefit(1000L, 100L);
		CardBenefitBrand brand = brand(1000L, "스타벅스");
		CardBenefitTier tier = tier(2000L, 1000L);
		CardBenefitUsage dailyUsage = usage(10L, 1000L, 100, LocalDateTime.of(2026, 5, 20, 8, 30));
		CardBenefitUsage monthlyUsage = usage(10L, 1000L, 200, LocalDateTime.of(2026, 5, 10, 12, 0));
		CardBenefitUsage yearlyUsage = usage(10L, 1000L, 300, LocalDateTime.of(2026, 1, 5, 12, 0));

		when(cardRegisteredRepository.findPaymentAvailableCards(1L, CardStatus.ACTIVE)).thenReturn(List.of(card));
		when(cardProductRepository.findAllById(any())).thenReturn(List.of(product));
		when(cardPerformanceRepository.findByUserIdAndYearMonthAndCardIdIn(eq(1L), eq("202605"), any()))
			.thenReturn(List.of(performance));
		when(cardBenefitRepository.findByCardProductIdInOrderByCardProductIdAscPriorityDescBenefitIdAsc(any()))
			.thenReturn(List.of(benefit));
		when(cardBenefitBrandRepository.findByBenefitIdInOrderByBrandNameAsc(List.of(1000L)))
			.thenReturn(List.of(brand));
		when(cardBenefitTierRepository.findByBenefitIdInOrderByMinPrevMonthUsageAsc(List.of(1000L)))
			.thenReturn(List.of(tier));
		when(cardBenefitUsageRepository
			.findByUserIdAndCardIdInAndBenefitIdInAndStatusAndApprovedAtGreaterThanEqualAndApprovedAtLessThan(
				eq(1L),
				any(),
				any(),
				eq(CardBenefitUsageStatus.APPROVED),
				any(),
				any()
			))
			.thenReturn(List.of(dailyUsage, monthlyUsage, yearlyUsage));

		InternalRecommendationSourceResponse response = internalCardService.getRecommendationSource(1L, "202605");

		assertThat(response.getUserId()).isEqualTo(1L);
		assertThat(response.getYearMonth()).isEqualTo("202605");
		assertThat(response.getCards()).hasSize(1);
		assertThat(response.getCards().getFirst().getCardId()).isEqualTo(10L);
		assertThat(response.getCards().getFirst().getPerformanceAmount()).isEqualTo(350000L);
		assertThat(response.getCards().getFirst().getBenefits()).hasSize(1);
		assertThat(response.getCards().getFirst().getBenefits().getFirst().getBrandNames())
			.containsExactly("스타벅스");
		assertThat(response.getCards().getFirst().getBenefits().getFirst().getUsage().getDailyAmount())
			.isEqualTo(100L);
		assertThat(response.getCards().getFirst().getBenefits().getFirst().getUsage().getDailyCount())
			.isEqualTo(1L);
		assertThat(response.getCards().getFirst().getBenefits().getFirst().getUsage().getMonthlyAmount())
			.isEqualTo(300L);
		assertThat(response.getCards().getFirst().getBenefits().getFirst().getUsage().getMonthlyCount())
			.isEqualTo(2L);
		assertThat(response.getCards().getFirst().getBenefits().getFirst().getUsage().getYearlyAmount())
			.isEqualTo(600L);
		assertThat(response.getCards().getFirst().getBenefits().getFirst().getUsage().getYearlyCount())
			.isEqualTo(3L);
		assertThat(response.getCards().getFirst().getBenefits().getFirst().getTiers()).hasSize(1);
	}

	@Test
	void getRecommendationSourceReturnsEmptyCardsWhenPaymentAvailableCardDoesNotExist() {
		when(cardRegisteredRepository.findPaymentAvailableCards(1L, CardStatus.ACTIVE)).thenReturn(List.of());

		InternalRecommendationSourceResponse response = internalCardService.getRecommendationSource(1L, "202605");

		assertThat(response.getCards()).isEmpty();
		verify(cardProductRepository, never()).findAllById(any());
	}

	@Test
	void getRecommendationSourceFailsWhenYearMonthIsInvalid() {
		assertThatThrownBy(() -> internalCardService.getRecommendationSource(1L, "202613"))
			.isInstanceOf(InvalidYearMonthException.class);

		verify(cardRegisteredRepository, never()).findPaymentAvailableCards(any(), any());
	}

	@Test
	void deactivateAllDeletesCardsAfterBillingKeyDeactivationInOrder() {
		CardRegistered activeCard = card(
			10L,
			1L,
			100L,
			CardStatus.ACTIVE,
			true,
			billingKeyCryptoService.encrypt("active-billing-key")
		);
		CardRegistered pausedCard = card(
			11L,
			1L,
			101L,
			CardStatus.PAUSED,
			false,
			billingKeyCryptoService.encrypt("paused-billing-key")
		);
		CardRegistered registeringCard = card(12L, 1L, 102L, CardStatus.REGISTERING, false, null);

		when(cardRegisteredRepository.findByUserIdAndStatusNotOrderByCreatedAtAscCardIdAsc(1L, CardStatus.DELETED))
			.thenReturn(List.of(activeCard, pausedCard, registeringCard));
		when(billingKeyServiceClient.delete(any()))
			.thenReturn(deleteResponse(10L, "100"))
			.thenReturn(deleteResponse(11L, "100"));

		InternalDeactivateCardsResponse response = internalCardService.deactivateAll(1L);

		assertThat(response.getUserId()).isEqualTo(1L);
		assertThat(response.getDeactivatedCount()).isEqualTo(3);

		ArgumentCaptor<BillingKeyDeleteRequest> deleteRequestCaptor =
			ArgumentCaptor.forClass(BillingKeyDeleteRequest.class);
		verify(billingKeyServiceClient, times(2)).delete(deleteRequestCaptor.capture());
		assertThat(deleteRequestCaptor.getAllValues())
			.extracting(BillingKeyDeleteRequest::payCardId)
			.containsExactly(10L, 11L);
		assertThat(deleteRequestCaptor.getAllValues())
			.extracting(BillingKeyDeleteRequest::billingKey)
			.containsExactly("active-billing-key", "paused-billing-key");

		ArgumentCaptor<LocalDateTime> deletedAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
		verify(activeCard).delete(deletedAtCaptor.capture());
		verify(pausedCard).delete(deletedAtCaptor.getValue());
		verify(registeringCard).delete(deletedAtCaptor.getValue());
		verify(cardRegisteredRepository).save(activeCard);
		verify(cardRegisteredRepository).save(pausedCard);
		verify(cardRegisteredRepository).save(registeringCard);
	}

	@Test
	void deactivateAllTreatsBillingKeyNotFoundAsIdempotentSuccess() {
		CardRegistered activeCard = card(10L, 1L, 100L, CardStatus.ACTIVE, false, "encrypted-key");
		FeignException notFound = mock(FeignException.class);

		when(cardRegisteredRepository.findByUserIdAndStatusNotOrderByCreatedAtAscCardIdAsc(1L, CardStatus.DELETED))
			.thenReturn(List.of(activeCard));
		when(notFound.status()).thenReturn(404);
		when(billingKeyServiceClient.delete(any())).thenThrow(notFound);

		InternalDeactivateCardsResponse response = internalCardService.deactivateAll(1L);

		assertThat(response.getDeactivatedCount()).isEqualTo(1);
		verify(activeCard).delete(LocalDateTime.now(FIXED_CLOCK));
		verify(cardRegisteredRepository).save(activeCard);
	}

	@Test
	void deactivateAllFailsWhenBillingKeyDeleteReturnsClientErrorExceptNotFound() {
		CardRegistered activeCard = card(10L, 1L, 100L, CardStatus.ACTIVE, false, "encrypted-key");
		FeignException badRequest = mock(FeignException.class);

		when(cardRegisteredRepository.findByUserIdAndStatusNotOrderByCreatedAtAscCardIdAsc(1L, CardStatus.DELETED))
			.thenReturn(List.of(activeCard));
		when(badRequest.status()).thenReturn(400);
		when(billingKeyServiceClient.delete(any())).thenThrow(badRequest);

		assertThatThrownBy(() -> internalCardService.deactivateAll(1L))
			.isInstanceOf(BillingKeyDeactivationFailedException.class);

		verify(activeCard, never()).delete(any());
		verify(cardRegisteredRepository, never()).save(activeCard);
	}

	@Test
	void deactivateAllDeletesLiveCardWithoutBillingKeyWithoutBillingKeyServiceCall() {
		CardRegistered activeCard = card(10L, 1L, 100L, CardStatus.ACTIVE, false, " ");

		when(cardRegisteredRepository.findByUserIdAndStatusNotOrderByCreatedAtAscCardIdAsc(1L, CardStatus.DELETED))
			.thenReturn(List.of(activeCard));

		InternalDeactivateCardsResponse response = internalCardService.deactivateAll(1L);

		assertThat(response.getDeactivatedCount()).isEqualTo(1);
		verify(billingKeyServiceClient, never()).delete(any());
		verify(activeCard).delete(LocalDateTime.now(FIXED_CLOCK));
		verify(cardRegisteredRepository).save(activeCard);
	}

	@Test
	void deactivateAllStopsWhenEncryptedBillingKeyCannotBeDecrypted() {
		CardRegistered activeCard = card(10L, 1L, 100L, CardStatus.ACTIVE, false, "enc:v1:invalid-base64");

		when(cardRegisteredRepository.findByUserIdAndStatusNotOrderByCreatedAtAscCardIdAsc(1L, CardStatus.DELETED))
			.thenReturn(List.of(activeCard));

		assertThatThrownBy(() -> internalCardService.deactivateAll(1L))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage("billing key decryption failed");

		verify(billingKeyServiceClient, never()).delete(any());
		verify(activeCard, never()).delete(any());
		verify(cardRegisteredRepository, never()).save(activeCard);
	}

	@Test
	void deactivateAllStopsWhenBillingKeyDeleteReturnsNonSuccess() {
		CardRegistered firstCard = card(10L, 1L, 100L, CardStatus.ACTIVE, false, "first-key");
		CardRegistered failedCard = card(11L, 1L, 101L, CardStatus.PAUSED, false, "failed-key");
		CardRegistered remainingCard = card(12L, 1L, 102L, CardStatus.EXPIRED, false, "remaining-key");

		when(cardRegisteredRepository.findByUserIdAndStatusNotOrderByCreatedAtAscCardIdAsc(1L, CardStatus.DELETED))
			.thenReturn(List.of(firstCard, failedCard, remainingCard));
		when(billingKeyServiceClient.delete(any()))
			.thenReturn(deleteResponse(10L, "100"))
			.thenReturn(deleteResponse(11L, "999"));

		assertThatThrownBy(() -> internalCardService.deactivateAll(1L))
			.isInstanceOf(BillingKeyDeactivationFailedException.class);

		verify(firstCard).delete(LocalDateTime.now(FIXED_CLOCK));
		verify(cardRegisteredRepository).save(firstCard);
		verify(failedCard, never()).delete(any());
		verify(remainingCard, never()).delete(any());
		verify(billingKeyServiceClient, times(2)).delete(any());
	}

	@Test
	void deactivateAllFailsWhenBillingKeyServiceIsUnavailable() {
		CardRegistered activeCard = card(10L, 1L, 100L, CardStatus.ACTIVE, false, "encrypted-key");
		FeignException unavailable = mock(FeignException.class);

		when(cardRegisteredRepository.findByUserIdAndStatusNotOrderByCreatedAtAscCardIdAsc(1L, CardStatus.DELETED))
			.thenReturn(List.of(activeCard));
		when(unavailable.status()).thenReturn(503);
		when(billingKeyServiceClient.delete(any())).thenThrow(unavailable);

		assertThatThrownBy(() -> internalCardService.deactivateAll(1L))
			.isInstanceOf(BillingKeyServiceUnavailableException.class);

		verify(activeCard, never()).delete(any());
		verify(cardRegisteredRepository, never()).save(activeCard);
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
		lenient().when(card.getEncryptedBillingKey()).thenReturn(billingKey);
		lenient().when(card.getStatus()).thenReturn(status);
		lenient().when(card.isDefaultCard()).thenReturn(defaultCard);
		lenient().when(card.isDeleted()).thenReturn(status == CardStatus.DELETED);
		lenient().when(card.isRegistering()).thenReturn(status == CardStatus.REGISTERING);
		lenient().when(card.isActive()).thenReturn(status == CardStatus.ACTIVE);
		lenient().when(card.hasBillingKey()).thenReturn(billingKey != null && !billingKey.isBlank());
		return card;
	}

	private BillingKeyDeleteResponse deleteResponse(Long payCardId, String responseCode) {
		return new BillingKeyDeleteResponse(payCardId, "billing-key", responseCode, "OK");
	}

	private BillingKeyCryptoService billingKeyCryptoService() {
		BillingKeyCryptoService service = new BillingKeyCryptoService("0123456789abcdef");
		service.init();
		return service;
	}

	private CardProduct product(Long cardProductId, String cardCompany, String cardName) {
		CardProduct product = mock(CardProduct.class);
		lenient().when(product.getCardProductId()).thenReturn(cardProductId);
		lenient().when(product.getCardCompany()).thenReturn(cardCompany);
		lenient().when(product.getCardName()).thenReturn(cardName);
		return product;
	}

	private CardPerformance performance(Long cardId, Long amount) {
		CardPerformance performance = mock(CardPerformance.class);
		when(performance.getCardId()).thenReturn(cardId);
		when(performance.getAmount()).thenReturn(amount);
		return performance;
	}

	private CardBenefit benefit(Long benefitId, Long cardProductId) {
		CardBenefit benefit = mock(CardBenefit.class);
		lenient().when(benefit.getBenefitId()).thenReturn(benefitId);
		lenient().when(benefit.getCardProductId()).thenReturn(cardProductId);
		lenient().when(benefit.getServiceCategory()).thenReturn(ServiceCategory.CAFE);
		lenient().when(benefit.getBenefitType()).thenReturn(BenefitType.DISCOUNT);
		lenient().when(benefit.getMinAmount()).thenReturn(10000L);
		lenient().when(benefit.getTimeStart()).thenReturn(LocalTime.of(9, 0));
		lenient().when(benefit.getTimeEnd()).thenReturn(LocalTime.of(18, 0));
		lenient().when(benefit.getDayCondition()).thenReturn(DayCondition.WEEKDAY);
		lenient().when(benefit.getBenefitDesc()).thenReturn("커피 할인");
		return benefit;
	}

	private CardBenefitBrand brand(Long benefitId, String brandName) {
		CardBenefitBrand brand = mock(CardBenefitBrand.class);
		when(brand.getBenefitId()).thenReturn(benefitId);
		when(brand.getBrandName()).thenReturn(brandName);
		return brand;
	}

	private CardBenefitTier tier(Long tierId, Long benefitId) {
		CardBenefitTier tier = mock(CardBenefitTier.class);
		lenient().when(tier.getTierId()).thenReturn(tierId);
		lenient().when(tier.getBenefitId()).thenReturn(benefitId);
		lenient().when(tier.getMinPrevMonthUsage()).thenReturn(300000L);
		lenient().when(tier.getRate()).thenReturn(new BigDecimal("10.000"));
		lenient().when(tier.getMonthlyLimitAmount()).thenReturn(5000L);
		return tier;
	}

	private CardBenefitUsage usage(Long cardId, Long benefitId, Integer benefitAmount, LocalDateTime approvedAt) {
		CardBenefitUsage usage = mock(CardBenefitUsage.class);
		when(usage.getCardId()).thenReturn(cardId);
		when(usage.getBenefitId()).thenReturn(benefitId);
		when(usage.getBenefitAmount()).thenReturn(benefitAmount);
		when(usage.getApprovedAt()).thenReturn(approvedAt);
		return usage;
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
