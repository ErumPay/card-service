package com.erumpay.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.erumpay.card.exception.BillingKeyNotFoundException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

	private InternalCardService internalCardService;

	@BeforeEach
	void setUp() {
		internalCardService = new InternalCardService(
			cardRegisteredRepository,
			cardProductRepository,
			cardPerformanceRepository,
			cardBenefitRepository,
			cardBenefitBrandRepository,
			cardBenefitTierRepository,
			cardBenefitUsageRepository,
			FIXED_CLOCK,
			new YearMonthValidator()
		);
	}

	@Test
	void getBillingKeyReturnsEncryptedBillingKeyForActiveOwnedCard() {
		CardRegistered card = card(10L, 1L, 100L, CardStatus.ACTIVE, false, "encrypted-key");
		when(cardRegisteredRepository.findByCardIdAndUserIdAndStatusNot(10L, 1L, CardStatus.DELETED))
			.thenReturn(Optional.of(card));

		InternalBillingKeyResponse response = internalCardService.getBillingKey(1L, 10L);

		assertThat(response.getCardId()).isEqualTo(10L);
		assertThat(response.getUserId()).isEqualTo(1L);
		assertThat(response.getEncryptedBillingKey()).isEqualTo("encrypted-key");
		assertThat(response.toString()).doesNotContain("encrypted-key");
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
	void deactivateAllDeletesOnlyNonDeletedCardsAndReturnsCount() {
		CardRegistered activeCard = card(10L, 1L, 100L, CardStatus.ACTIVE, true, "encrypted-key");
		CardRegistered pausedCard = card(11L, 1L, 101L, CardStatus.PAUSED, false, "encrypted-key");

		when(cardRegisteredRepository.findByUserIdAndStatusNot(1L, CardStatus.DELETED))
			.thenReturn(List.of(activeCard, pausedCard));

		InternalDeactivateCardsResponse response = internalCardService.deactivateAll(1L);

		assertThat(response.getUserId()).isEqualTo(1L);
		assertThat(response.getDeactivatedCount()).isEqualTo(2);

		ArgumentCaptor<LocalDateTime> deletedAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
		verify(activeCard).delete(deletedAtCaptor.capture());
		verify(pausedCard).delete(deletedAtCaptor.getValue());
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
}
