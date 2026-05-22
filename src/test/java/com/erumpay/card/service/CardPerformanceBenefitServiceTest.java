package com.erumpay.card.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.erumpay.card.domain.entity.CardBenefit;
import com.erumpay.card.domain.entity.CardBenefitBrand;
import com.erumpay.card.domain.entity.CardBenefitTier;
import com.erumpay.card.domain.entity.CardPerformance;
import com.erumpay.card.domain.entity.CardRegistered;
import com.erumpay.card.domain.enums.BenefitType;
import com.erumpay.card.domain.enums.CardStatus;
import com.erumpay.card.domain.enums.DayCondition;
import com.erumpay.card.domain.enums.ServiceCategory;
import com.erumpay.card.dto.CardBenefitResponse;
import com.erumpay.card.dto.CardPerformanceResponse;
import com.erumpay.card.exception.CardNotFoundException;
import com.erumpay.card.exception.InvalidYearMonthException;
import com.erumpay.card.repository.CardBenefitBrandRepository;
import com.erumpay.card.repository.CardBenefitRepository;
import com.erumpay.card.repository.CardBenefitTierRepository;
import com.erumpay.card.repository.CardPerformanceRepository;
import com.erumpay.card.repository.CardRegisteredRepository;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CardPerformanceBenefitServiceTest {

	@Mock
	private CardRegisteredRepository cardRegisteredRepository;

	@Mock
	private CardPerformanceRepository cardPerformanceRepository;

	@Mock
	private CardBenefitRepository cardBenefitRepository;

	@Mock
	private CardBenefitBrandRepository cardBenefitBrandRepository;

	@Mock
	private CardBenefitTierRepository cardBenefitTierRepository;

	private CardPerformanceBenefitService cardPerformanceBenefitService;

	@BeforeEach
	void setUp() {
		cardPerformanceBenefitService = new CardPerformanceBenefitService(
			cardRegisteredRepository,
			cardPerformanceRepository,
			cardBenefitRepository,
			cardBenefitBrandRepository,
			cardBenefitTierRepository,
			new YearMonthValidator()
		);
	}

	@Test
	void getPerformanceReturnsSavedAmount() {
		CardRegistered card = card(10L, 1L, 100L);
		CardPerformance performance = performance(350000L);

		when(cardRegisteredRepository.findByCardIdAndUserIdAndStatusNot(10L, 1L, CardStatus.DELETED))
			.thenReturn(Optional.of(card));
		when(cardPerformanceRepository.findByCardIdAndUserIdAndYearMonth(10L, 1L, "202605"))
			.thenReturn(Optional.of(performance));

		CardPerformanceResponse response = cardPerformanceBenefitService.getPerformance(1L, 10L, "202605");

		assertThat(response.getCardId()).isEqualTo(10L);
		assertThat(response.getYearMonth()).isEqualTo("202605");
		assertThat(response.getAmount()).isEqualTo(350000L);
	}

	@Test
	void getPerformanceReturnsZeroWhenPerformanceDoesNotExist() {
		CardRegistered card = card(10L, 1L, 100L);

		when(cardRegisteredRepository.findByCardIdAndUserIdAndStatusNot(10L, 1L, CardStatus.DELETED))
			.thenReturn(Optional.of(card));
		when(cardPerformanceRepository.findByCardIdAndUserIdAndYearMonth(10L, 1L, "202605"))
			.thenReturn(Optional.empty());

		CardPerformanceResponse response = cardPerformanceBenefitService.getPerformance(1L, 10L, "202605");

		assertThat(response.getAmount()).isZero();
	}

	@Test
	void getPerformanceFailsWhenYearMonthHasInvalidMonth() {
		assertThatThrownBy(() -> cardPerformanceBenefitService.getPerformance(1L, 10L, "202613"))
			.isInstanceOf(InvalidYearMonthException.class);

		verify(cardRegisteredRepository, never()).findByCardIdAndUserIdAndStatusNot(any(), any(), any());
	}

	@Test
	void getPerformanceFailsWhenCardDoesNotBelongToUser() {
		when(cardRegisteredRepository.findByCardIdAndUserIdAndStatusNot(10L, 1L, CardStatus.DELETED))
			.thenReturn(Optional.empty());

		assertThatThrownBy(() -> cardPerformanceBenefitService.getPerformance(1L, 10L, "202605"))
			.isInstanceOf(CardNotFoundException.class);
	}

	@Test
	void getBenefitsReturnsBenefitsWithBrandsAndTiers() {
		CardRegistered card = card(10L, 1L, 100L);
		CardBenefit benefit = benefit(1000L, 100L, ServiceCategory.CAFE, BenefitType.DISCOUNT);
		CardBenefitBrand brand = brand(1000L, "스타벅스");
		CardBenefitTier tier = tier(2000L, 1000L, 300000L);

		when(cardRegisteredRepository.findByCardIdAndUserIdAndStatusNot(10L, 1L, CardStatus.DELETED))
			.thenReturn(Optional.of(card));
		when(cardBenefitRepository.findByCardProductIdOrderByPriorityDescBenefitIdAsc(100L))
			.thenReturn(List.of(benefit));
		when(cardBenefitBrandRepository.findByBenefitIdInOrderByBrandNameAsc(List.of(1000L)))
			.thenReturn(List.of(brand));
		when(cardBenefitTierRepository.findByBenefitIdInOrderByMinPrevMonthUsageAsc(List.of(1000L)))
			.thenReturn(List.of(tier));

		List<CardBenefitResponse> responses = cardPerformanceBenefitService.getBenefits(1L, 10L);

		assertThat(responses).hasSize(1);
		CardBenefitResponse response = responses.getFirst();
		assertThat(response.getBenefitId()).isEqualTo(1000L);
		assertThat(response.getServiceCategory()).isEqualTo("CAFE");
		assertThat(response.getBenefitType()).isEqualTo("DISCOUNT");
		assertThat(response.getMinAmount()).isEqualTo(10000L);
		assertThat(response.getTimeStart()).isEqualTo("09:00");
		assertThat(response.getTimeEnd()).isEqualTo("18:00");
		assertThat(response.getDayCondition()).isEqualTo("WEEKDAY");
		assertThat(response.getBrandNames()).containsExactly("스타벅스");
		assertThat(response.getTiers()).hasSize(1);
		assertThat(response.getTiers().getFirst().getTierId()).isEqualTo(2000L);
		assertThat(response.getTiers().getFirst().getMinPrevMonthUsage()).isEqualTo(300000L);
		assertThat(response.getTiers().getFirst().getRate()).isEqualByComparingTo("10.000");
	}

	@Test
	void getBenefitsReturnsEmptyBrandNamesWhenBenefitHasNoBrandRows() {
		CardRegistered card = card(10L, 1L, 100L);
		CardBenefit benefit = benefit(1000L, 100L, ServiceCategory.CAFE, BenefitType.DISCOUNT);

		when(cardRegisteredRepository.findByCardIdAndUserIdAndStatusNot(10L, 1L, CardStatus.DELETED))
			.thenReturn(Optional.of(card));
		when(cardBenefitRepository.findByCardProductIdOrderByPriorityDescBenefitIdAsc(100L))
			.thenReturn(List.of(benefit));
		when(cardBenefitBrandRepository.findByBenefitIdInOrderByBrandNameAsc(List.of(1000L)))
			.thenReturn(List.of());
		when(cardBenefitTierRepository.findByBenefitIdInOrderByMinPrevMonthUsageAsc(List.of(1000L)))
			.thenReturn(List.of());

		List<CardBenefitResponse> responses = cardPerformanceBenefitService.getBenefits(1L, 10L);

		assertThat(responses.getFirst().getBrandNames()).isEmpty();
		assertThat(responses.getFirst().getTiers()).isEmpty();
	}

	private CardRegistered card(Long cardId, Long userId, Long cardProductId) {
		CardRegistered card = mock(CardRegistered.class);
		lenient().when(card.getCardId()).thenReturn(cardId);
		lenient().when(card.getUserId()).thenReturn(userId);
		lenient().when(card.getCardProductId()).thenReturn(cardProductId);
		return card;
	}

	private CardPerformance performance(Long amount) {
		CardPerformance performance = mock(CardPerformance.class);
		when(performance.getAmount()).thenReturn(amount);
		return performance;
	}

	private CardBenefit benefit(
		Long benefitId,
		Long cardProductId,
		ServiceCategory serviceCategory,
		BenefitType benefitType
	) {
		CardBenefit benefit = mock(CardBenefit.class);
		lenient().when(benefit.getBenefitId()).thenReturn(benefitId);
		lenient().when(benefit.getCardProductId()).thenReturn(cardProductId);
		lenient().when(benefit.getServiceCategory()).thenReturn(serviceCategory);
		lenient().when(benefit.getBenefitType()).thenReturn(benefitType);
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

	private CardBenefitTier tier(Long tierId, Long benefitId, Long minPrevMonthUsage) {
		CardBenefitTier tier = mock(CardBenefitTier.class);
		lenient().when(tier.getTierId()).thenReturn(tierId);
		lenient().when(tier.getBenefitId()).thenReturn(benefitId);
		lenient().when(tier.getMinPrevMonthUsage()).thenReturn(minPrevMonthUsage);
		lenient().when(tier.getMaxPrevMonthUsage()).thenReturn(null);
		lenient().when(tier.getRate()).thenReturn(new BigDecimal("10.000"));
		lenient().when(tier.getFlatAmount()).thenReturn(null);
		lenient().when(tier.getMaxBenefitPerUse()).thenReturn(5000L);
		lenient().when(tier.getDailyLimitCount()).thenReturn(null);
		lenient().when(tier.getDailyLimitAmount()).thenReturn(null);
		lenient().when(tier.getMonthlyLimitCount()).thenReturn(null);
		lenient().when(tier.getMonthlyLimitAmount()).thenReturn(5000L);
		lenient().when(tier.getYearlyLimitCount()).thenReturn(null);
		lenient().when(tier.getYearlyLimitAmount()).thenReturn(null);
		lenient().when(tier.getTierDesc()).thenReturn(null);
		return tier;
	}
}
