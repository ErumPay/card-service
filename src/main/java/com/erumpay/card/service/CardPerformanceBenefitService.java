package com.erumpay.card.service;

import com.erumpay.card.domain.entity.CardBenefit;
import com.erumpay.card.domain.entity.CardBenefitBrand;
import com.erumpay.card.domain.entity.CardBenefitTier;
import com.erumpay.card.domain.entity.CardPerformance;
import com.erumpay.card.domain.entity.CardRegistered;
import com.erumpay.card.domain.enums.CardStatus;
import com.erumpay.card.dto.CardBenefitResponse;
import com.erumpay.card.dto.CardBenefitResponse.CardBenefitTierResponse;
import com.erumpay.card.dto.CardPerformanceResponse;
import com.erumpay.card.exception.CardNotFoundException;
import com.erumpay.card.repository.CardBenefitBrandRepository;
import com.erumpay.card.repository.CardBenefitRepository;
import com.erumpay.card.repository.CardBenefitTierRepository;
import com.erumpay.card.repository.CardPerformanceRepository;
import com.erumpay.card.repository.CardRegisteredRepository;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CardPerformanceBenefitService {

	private final CardRegisteredRepository cardRegisteredRepository;
	private final CardPerformanceRepository cardPerformanceRepository;
	private final CardBenefitRepository cardBenefitRepository;
	private final CardBenefitBrandRepository cardBenefitBrandRepository;
	private final CardBenefitTierRepository cardBenefitTierRepository;
	private final YearMonthValidator yearMonthValidator;

	// [be] 이준혁 260521 2028 | 삭제되지 않은 사용자 카드의 월 실적을 조회한다. 실적 row가 없으면 0원으로 응답한다.
	@Transactional(readOnly = true)
	public CardPerformanceResponse getPerformance(Long userId, Long cardId, String yearMonth) {
		yearMonthValidator.validate(yearMonth);
		CardRegistered card = findOwnedNonDeletedCard(userId, cardId);

		Long amount = cardPerformanceRepository.findByCardIdAndUserIdAndYearMonth(card.getCardId(), userId, yearMonth)
			.map(CardPerformance::getAmount)
			.orElse(0L);

		return CardPerformanceResponse.builder()
			.cardId(card.getCardId())
			.yearMonth(yearMonth)
			.amount(amount)
			.build();
	}

	// [be] 이준혁 260521 2028 | 카드 상품의 혜택, 브랜드 제한, 실적 구간을 함께 조회해 응답 형태로 묶는다.
	@Transactional(readOnly = true)
	public List<CardBenefitResponse> getBenefits(Long userId, Long cardId) {
		CardRegistered card = findOwnedNonDeletedCard(userId, cardId);
		List<CardBenefit> benefits = cardBenefitRepository
			.findByCardProductIdOrderByPriorityDescBenefitIdAsc(card.getCardProductId());
		if (benefits.isEmpty()) {
			return List.of();
		}

		List<Long> benefitIds = benefits.stream()
			.map(CardBenefit::getBenefitId)
			.toList();
		Map<Long, List<String>> brandNamesByBenefitId = findBrandNamesByBenefitId(benefitIds);
		Map<Long, List<CardBenefitTier>> tiersByBenefitId = findTiersByBenefitId(benefitIds);

		return benefits.stream()
			.map(benefit -> toBenefitResponse(
				benefit,
				brandNamesByBenefitId.getOrDefault(benefit.getBenefitId(), List.of()),
				tiersByBenefitId.getOrDefault(benefit.getBenefitId(), List.of())
			))
			.toList();
	}

	private CardRegistered findOwnedNonDeletedCard(Long userId, Long cardId) {
		return cardRegisteredRepository.findByCardIdAndUserIdAndStatusNot(cardId, userId, CardStatus.DELETED)
			.orElseThrow(CardNotFoundException::new);
	}

	private Map<Long, List<String>> findBrandNamesByBenefitId(Collection<Long> benefitIds) {
		return cardBenefitBrandRepository.findByBenefitIdInOrderByBrandNameAsc(benefitIds).stream()
			.collect(Collectors.groupingBy(
				CardBenefitBrand::getBenefitId,
				Collectors.mapping(CardBenefitBrand::getBrandName, Collectors.toList())
			));
	}

	private Map<Long, List<CardBenefitTier>> findTiersByBenefitId(Collection<Long> benefitIds) {
		return cardBenefitTierRepository.findByBenefitIdInOrderByMinPrevMonthUsageAsc(benefitIds).stream()
			.collect(Collectors.groupingBy(CardBenefitTier::getBenefitId));
	}

	private CardBenefitResponse toBenefitResponse(
		CardBenefit benefit,
		List<String> brandNames,
		List<CardBenefitTier> tiers
	) {
		return CardBenefitResponse.builder()
			.benefitId(benefit.getBenefitId())
			.serviceCategory(benefit.getServiceCategory().name())
			.benefitType(benefit.getBenefitType().name())
			.minAmount(benefit.getMinAmount())
			.timeStart(toTimeString(benefit.getTimeStart()))
			.timeEnd(toTimeString(benefit.getTimeEnd()))
			.dayCondition(benefit.getDayCondition().name())
			.benefitDesc(benefit.getBenefitDesc())
			.brandNames(brandNames)
			.tiers(tiers.stream()
				.map(this::toTierResponse)
				.toList())
			.build();
	}

	private CardBenefitTierResponse toTierResponse(CardBenefitTier tier) {
		return CardBenefitTierResponse.builder()
			.tierId(tier.getTierId())
			.minPrevMonthUsage(tier.getMinPrevMonthUsage())
			.maxPrevMonthUsage(tier.getMaxPrevMonthUsage())
			.rate(tier.getRate())
			.flatAmount(tier.getFlatAmount())
			.maxBenefitPerUse(tier.getMaxBenefitPerUse())
			.dailyLimitCount(tier.getDailyLimitCount())
			.dailyLimitAmount(tier.getDailyLimitAmount())
			.monthlyLimitCount(tier.getMonthlyLimitCount())
			.monthlyLimitAmount(tier.getMonthlyLimitAmount())
			.yearlyLimitCount(tier.getYearlyLimitCount())
			.yearlyLimitAmount(tier.getYearlyLimitAmount())
			.tierDesc(tier.getTierDesc())
			.build();
	}

	private String toTimeString(LocalTime time) {
		return time == null ? null : time.toString();
	}
}
