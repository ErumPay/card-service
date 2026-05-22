package com.erumpay.card.service;

import com.erumpay.card.domain.entity.CardBenefit;
import com.erumpay.card.domain.entity.CardBenefitBrand;
import com.erumpay.card.domain.entity.CardBenefitTier;
import com.erumpay.card.domain.entity.CardPerformance;
import com.erumpay.card.domain.entity.CardProduct;
import com.erumpay.card.domain.entity.CardRegistered;
import com.erumpay.card.domain.enums.CardStatus;
import com.erumpay.card.dto.CardBenefitResponse;
import com.erumpay.card.dto.CardBenefitResponse.CardBenefitTierResponse;
import com.erumpay.card.dto.InternalBillingKeyResponse;
import com.erumpay.card.dto.InternalDeactivateCardsResponse;
import com.erumpay.card.dto.InternalDefaultCardResponse;
import com.erumpay.card.dto.InternalRecommendationSourceResponse;
import com.erumpay.card.dto.InternalRecommendationSourceResponse.InternalRecommendationCardResponse;
import com.erumpay.card.exception.BillingKeyNotFoundException;
import com.erumpay.card.exception.CardNotActiveException;
import com.erumpay.card.exception.CardNotFoundException;
import com.erumpay.card.repository.CardBenefitBrandRepository;
import com.erumpay.card.repository.CardBenefitRepository;
import com.erumpay.card.repository.CardBenefitTierRepository;
import com.erumpay.card.repository.CardPerformanceRepository;
import com.erumpay.card.repository.CardProductRepository;
import com.erumpay.card.repository.CardRegisteredRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InternalCardService {

	private final CardRegisteredRepository cardRegisteredRepository;
	private final CardProductRepository cardProductRepository;
	private final CardPerformanceRepository cardPerformanceRepository;
	private final CardBenefitRepository cardBenefitRepository;
	private final CardBenefitBrandRepository cardBenefitBrandRepository;
	private final CardBenefitTierRepository cardBenefitTierRepository;
	private final Clock clock;
	private final YearMonthValidator yearMonthValidator;

	// [be] 이준혁 260522 0902 | 결제 승인 전 사용할 billing key를 userId와 cardId 소유자 검증 후 반환한다.
	@Transactional(readOnly = true)
	public InternalBillingKeyResponse getBillingKey(Long userId, Long cardId) {
		CardRegistered card = findOwnedNonDeletedCard(userId, cardId);
		if (!card.isActive()) {
			throw new CardNotActiveException();
		}
		if (!card.hasBillingKey()) {
			throw new BillingKeyNotFoundException();
		}

		return InternalBillingKeyResponse.builder()
			.cardId(card.getCardId())
			.userId(card.getUserId())
			.cardProductId(card.getCardProductId())
			.encryptedBillingKey(card.getEncryptedBillingKey())
			.maskedNumber(card.getMaskedNumber())
			.build();
	}

	// [be] 이준혁 260522 0902 | 사용자의 ACTIVE 주카드를 조회한다. 조회 API에서는 주카드 누락 상태를 자동 복구하지 않는다.
	@Transactional(readOnly = true)
	public InternalDefaultCardResponse getDefaultCard(Long userId) {
		CardRegistered card = cardRegisteredRepository
			.findByUserIdAndDefaultCardTrueAndStatus(userId, CardStatus.ACTIVE)
			.orElseThrow(CardNotFoundException::new);
		CardProduct product = findProductById(card.getCardProductId());

		return InternalDefaultCardResponse.builder()
			.cardId(card.getCardId())
			.userId(card.getUserId())
			.cardProductId(card.getCardProductId())
			.maskedNumber(card.getMaskedNumber())
			.cardCompany(product.getCardCompany())
			.cardName(product.getCardName())
			.build();
	}

	// [be] 이준혁 260522 0902 | 추천 계산에 필요한 결제 가능 카드, 실적, 혜택 데이터를 billing key 없이 묶어 제공한다.
	@Transactional(readOnly = true)
	public InternalRecommendationSourceResponse getRecommendationSource(Long userId, String yearMonth) {
		yearMonthValidator.validate(yearMonth);
		List<CardRegistered> cards = cardRegisteredRepository.findPaymentAvailableCards(userId, CardStatus.ACTIVE);
		if (cards.isEmpty()) {
			return InternalRecommendationSourceResponse.builder()
				.userId(userId)
				.yearMonth(yearMonth)
				.cards(List.of())
				.build();
		}

		Map<Long, CardProduct> productsById = findProductsById(cards.stream()
			.map(CardRegistered::getCardProductId)
			.toList());
		Map<Long, Long> performanceAmountsByCardId = findPerformanceAmountsByCardId(userId, yearMonth, cards.stream()
			.map(CardRegistered::getCardId)
			.toList());
		Map<Long, List<CardBenefitResponse>> benefitsByProductId = findBenefitsByProductId(productsById.keySet());

		List<InternalRecommendationCardResponse> cardResponses = cards.stream()
			.map(card -> toRecommendationCardResponse(
				card,
				productsById.get(card.getCardProductId()),
				performanceAmountsByCardId.getOrDefault(card.getCardId(), 0L),
				benefitsByProductId.getOrDefault(card.getCardProductId(), List.of())
			))
			.toList();

		return InternalRecommendationSourceResponse.builder()
			.userId(userId)
			.yearMonth(yearMonth)
			.cards(cardResponses)
			.build();
	}

	// [be] 이준혁 260522 0902 | 회원탈퇴 시 사용자의 비삭제 카드를 모두 DELETED로 바꾸고 주카드 대체 지정은 수행하지 않는다.
	@Transactional
	public InternalDeactivateCardsResponse deactivateAll(Long userId) {
		List<CardRegistered> cards = cardRegisteredRepository.findByUserIdAndStatusNot(userId, CardStatus.DELETED);
		LocalDateTime deletedAt = LocalDateTime.now(clock);
		cards.forEach(card -> card.delete(deletedAt));

		return InternalDeactivateCardsResponse.builder()
			.userId(userId)
			.deactivatedCount(cards.size())
			.build();
	}

	private CardRegistered findOwnedNonDeletedCard(Long userId, Long cardId) {
		return cardRegisteredRepository.findByCardIdAndUserIdAndStatusNot(cardId, userId, CardStatus.DELETED)
			.orElseThrow(CardNotFoundException::new);
	}

	private CardProduct findProductById(Long cardProductId) {
		return cardProductRepository.findById(cardProductId)
			.orElseThrow(() -> new IllegalStateException("Card product not found for registered card."));
	}

	private Map<Long, CardProduct> findProductsById(Collection<Long> cardProductIds) {
		return cardProductRepository.findAllById(cardProductIds).stream()
			.collect(Collectors.toMap(CardProduct::getCardProductId, Function.identity()));
	}

	private Map<Long, Long> findPerformanceAmountsByCardId(Long userId, String yearMonth, Collection<Long> cardIds) {
		return cardPerformanceRepository.findByUserIdAndYearMonthAndCardIdIn(userId, yearMonth, cardIds).stream()
			.collect(Collectors.toMap(CardPerformance::getCardId, CardPerformance::getAmount));
	}

	private Map<Long, List<CardBenefitResponse>> findBenefitsByProductId(Collection<Long> cardProductIds) {
		List<CardBenefit> benefits = cardBenefitRepository
			.findByCardProductIdInOrderByCardProductIdAscPriorityDescBenefitIdAsc(cardProductIds);
		if (benefits.isEmpty()) {
			return Map.of();
		}

		List<Long> benefitIds = benefits.stream()
			.map(CardBenefit::getBenefitId)
			.toList();
		Map<Long, List<String>> brandNamesByBenefitId = findBrandNamesByBenefitId(benefitIds);
		Map<Long, List<CardBenefitTier>> tiersByBenefitId = findTiersByBenefitId(benefitIds);

		return benefits.stream()
			.collect(Collectors.groupingBy(
				CardBenefit::getCardProductId,
				Collectors.mapping(
					benefit -> toBenefitResponse(
						benefit,
						brandNamesByBenefitId.getOrDefault(benefit.getBenefitId(), List.of()),
						tiersByBenefitId.getOrDefault(benefit.getBenefitId(), List.of())
					),
					Collectors.toList()
				)
			));
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

	private InternalRecommendationCardResponse toRecommendationCardResponse(
		CardRegistered card,
		CardProduct product,
		Long performanceAmount,
		List<CardBenefitResponse> benefits
	) {
		if (product == null) {
			throw new IllegalStateException("Card product not found for registered card.");
		}

		return InternalRecommendationCardResponse.builder()
			.cardId(card.getCardId())
			.cardProductId(card.getCardProductId())
			.cardCompany(product.getCardCompany())
			.cardName(product.getCardName())
			.maskedNumber(card.getMaskedNumber())
			.isDefault(card.isDefaultCard())
			.performanceAmount(performanceAmount)
			.benefits(benefits)
			.build();
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
