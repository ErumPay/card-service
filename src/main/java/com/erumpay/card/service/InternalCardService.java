package com.erumpay.card.service;

import com.erumpay.card.client.BillingKeyServiceClient;
import com.erumpay.card.domain.entity.CardBenefit;
import com.erumpay.card.domain.entity.CardBenefitBrand;
import com.erumpay.card.domain.entity.CardBenefitTier;
import com.erumpay.card.domain.entity.CardBenefitUsage;
import com.erumpay.card.domain.entity.CardPerformance;
import com.erumpay.card.domain.entity.CardProduct;
import com.erumpay.card.domain.entity.CardRegistered;
import com.erumpay.card.domain.enums.CardBenefitUsageStatus;
import com.erumpay.card.domain.enums.CardStatus;
import com.erumpay.card.dto.InternalBillingKeyResponse;
import com.erumpay.card.dto.InternalDeactivateCardsResponse;
import com.erumpay.card.dto.InternalDefaultCardResponse;
import com.erumpay.card.dto.InternalRecommendationSourceResponse;
import com.erumpay.card.dto.InternalRecommendationSourceResponse.InternalBenefitUsageResponse;
import com.erumpay.card.dto.InternalRecommendationSourceResponse.InternalRecommendationBenefitResponse;
import com.erumpay.card.dto.InternalRecommendationSourceResponse.InternalRecommendationBenefitTierResponse;
import com.erumpay.card.dto.InternalRecommendationSourceResponse.InternalRecommendationCardResponse;
import com.erumpay.card.dto.client.BillingKeyDeleteRequest;
import com.erumpay.card.dto.client.BillingKeyDeleteResponse;
import com.erumpay.card.exception.BillingKeyDeactivationFailedException;
import com.erumpay.card.exception.BillingKeyNotFoundException;
import com.erumpay.card.exception.BillingKeyServiceUnavailableException;
import com.erumpay.card.exception.CardNotActiveException;
import com.erumpay.card.exception.CardNotFoundException;
import com.erumpay.card.repository.CardBenefitBrandRepository;
import com.erumpay.card.repository.CardBenefitRepository;
import com.erumpay.card.repository.CardBenefitTierRepository;
import com.erumpay.card.repository.CardBenefitUsageRepository;
import com.erumpay.card.repository.CardPerformanceRepository;
import com.erumpay.card.repository.CardProductRepository;
import com.erumpay.card.repository.CardRegisteredRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalCardService {

	private static final String BILLING_KEY_DELETE_SUCCESS = "100";
	private static final String BILLING_KEY_NOT_FOUND_REASON = "BILLING_KEY_NOT_FOUND";
	private static final int HTTP_NOT_FOUND = 404;

	private final CardRegisteredRepository cardRegisteredRepository;
	private final CardProductRepository cardProductRepository;
	private final CardPerformanceRepository cardPerformanceRepository;
	private final CardBenefitRepository cardBenefitRepository;
	private final CardBenefitBrandRepository cardBenefitBrandRepository;
	private final CardBenefitTierRepository cardBenefitTierRepository;
	private final CardBenefitUsageRepository cardBenefitUsageRepository;
	private final BillingKeyServiceClient billingKeyServiceClient;
	private final Clock clock;
	private final YearMonthValidator yearMonthValidator;
	private final TransactionTemplate transactionTemplate;

	// [be] 이준혁 260522 0902 | 결제 승인 전 사용할 billing key를 userId와 cardId 소유자 검증 후 반환한다.
	@Transactional(readOnly = true)
	public InternalBillingKeyResponse getBillingKey(Long userId, Long cardId) {
		CardRegistered card = findOwnedNonDeletedCard(userId, cardId);
		if (card.isRegistering()) {
			throw new CardNotFoundException();
		}
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
			.billingKey(card.getEncryptedBillingKey())
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
		List<Long> cardIds = cards.stream()
			.map(CardRegistered::getCardId)
			.toList();
		Map<Long, Long> performanceAmountsByCardId = findPerformanceAmountsByCardId(userId, yearMonth, cardIds);
		RecommendationBenefitSources benefitSources = findBenefitSourcesByProductId(productsById.keySet());
		Map<BenefitUsageKey, InternalBenefitUsageResponse> usageByCardAndBenefit =
			findBenefitUsagesByCardAndBenefit(userId, cardIds, benefitSources.benefitIds());

		List<InternalRecommendationCardResponse> cardResponses = cards.stream()
			.map(card -> toRecommendationCardResponse(
				card,
				productsById.get(card.getCardProductId()),
				performanceAmountsByCardId.getOrDefault(card.getCardId(), 0L),
				benefitSources,
				usageByCardAndBenefit
			))
			.toList();

		return InternalRecommendationSourceResponse.builder()
			.userId(userId)
			.yearMonth(yearMonth)
			.cards(cardResponses)
			.build();
	}

	// [be] 이준혁 260522 0902 | 회원탈퇴 시 사용자의 비삭제 카드를 모두 DELETED로 바꾸고 주카드 대체 지정은 수행하지 않는다.
	public InternalDeactivateCardsResponse deactivateAll(Long userId) {
		List<CardRegistered> cards = cardRegisteredRepository
			.findByUserIdAndStatusNotOrderByCreatedAtAscCardIdAsc(userId, CardStatus.DELETED);
		int deactivatedCount = 0;
		for (CardRegistered card : cards) {
			if (deactivateCardForWithdraw(userId, card)) {
				deactivatedCount++;
			}
		}

		return InternalDeactivateCardsResponse.builder()
			.userId(userId)
			.deactivatedCount(deactivatedCount)
			.build();
	}

	private boolean deactivateCardForWithdraw(Long userId, CardRegistered card) {
		if (card.isDeleted()) {
			return false;
		}
		if (card.isRegistering()) {
			softDeleteCardForWithdraw(card);
			return true;
		}
		if (!card.hasBillingKey()) {
			log.error(
				"Live card has no billing key during deactivate-all. userId={}, cardId={}, status={}, reason={}",
				userId,
				card.getCardId(),
				card.getStatus(),
				BILLING_KEY_NOT_FOUND_REASON
			);
			softDeleteCardForWithdraw(card);
			return true;
		}

		deactivateBillingKeyForWithdraw(card);
		softDeleteCardForWithdraw(card);
		return true;
	}

	private void deactivateBillingKeyForWithdraw(CardRegistered card) {
		BillingKeyDeleteResponse response;
		try {
			response = billingKeyServiceClient.delete(
				new BillingKeyDeleteRequest(card.getCardId(), card.getEncryptedBillingKey())
			);
		} catch (FeignException exception) {
			if (exception.status() == HTTP_NOT_FOUND) {
				log.warn(
					"Billing key already inactive during deactivate-all. userId={}, cardId={}, status={}",
					card.getUserId(),
					card.getCardId(),
					card.getStatus()
				);
				return;
			}
			if (isBillingKeyServiceUnavailable(exception.status())) {
				throw new BillingKeyServiceUnavailableException(exception);
			}
			throw new BillingKeyDeactivationFailedException(exception);
		} catch (RuntimeException exception) {
			throw new BillingKeyServiceUnavailableException(exception);
		}

		if (response == null || !BILLING_KEY_DELETE_SUCCESS.equals(response.responseCode())) {
			throw new BillingKeyDeactivationFailedException();
		}
	}

	private boolean isBillingKeyServiceUnavailable(int status) {
		return status <= 0 || status >= 500;
	}

	private void softDeleteCardForWithdraw(CardRegistered card) {
		transactionTemplate.executeWithoutResult(status -> {
			card.delete(LocalDateTime.now(clock));
			cardRegisteredRepository.save(card);
		});
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

	private RecommendationBenefitSources findBenefitSourcesByProductId(Collection<Long> cardProductIds) {
		List<CardBenefit> benefits = cardBenefitRepository
			.findByCardProductIdInOrderByCardProductIdAscPriorityDescBenefitIdAsc(cardProductIds);
		if (benefits.isEmpty()) {
			return new RecommendationBenefitSources(Map.of(), Map.of(), Map.of(), List.of());
		}

		List<Long> benefitIds = benefits.stream()
			.map(CardBenefit::getBenefitId)
			.toList();
		Map<Long, List<String>> brandNamesByBenefitId = findBrandNamesByBenefitId(benefitIds);
		Map<Long, List<CardBenefitTier>> tiersByBenefitId = findTiersByBenefitId(benefitIds);

		Map<Long, List<CardBenefit>> benefitsByProductId = benefits.stream()
			.collect(Collectors.groupingBy(CardBenefit::getCardProductId));

		return new RecommendationBenefitSources(benefitsByProductId, brandNamesByBenefitId, tiersByBenefitId, benefitIds);
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

	// [be] 이준혁 260526 1104 | 추천 한도 계산에 필요한 혜택별 일/월/년 사용량을 현재 시각 기준으로 집계한다.
	private Map<BenefitUsageKey, InternalBenefitUsageResponse> findBenefitUsagesByCardAndBenefit(
		Long userId,
		Collection<Long> cardIds,
		Collection<Long> benefitIds
	) {
		if (cardIds.isEmpty() || benefitIds.isEmpty()) {
			return Map.of();
		}

		LocalDate today = LocalDate.now(clock);
		LocalDateTime dayStart = today.atStartOfDay();
		LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();
		LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
		LocalDateTime yearStart = today.withDayOfYear(1).atStartOfDay();

		List<CardBenefitUsage> usages = cardBenefitUsageRepository
			.findByUserIdAndCardIdInAndBenefitIdInAndStatusAndApprovedAtGreaterThanEqualAndApprovedAtLessThan(
				userId,
				cardIds,
				benefitIds,
				CardBenefitUsageStatus.APPROVED,
				yearStart,
				dayEnd
			);
		Map<BenefitUsageKey, BenefitUsageAccumulator> accumulators = new HashMap<>();

		usages.forEach(usage -> accumulators
			.computeIfAbsent(new BenefitUsageKey(usage.getCardId(), usage.getBenefitId()),
				key -> new BenefitUsageAccumulator())
			.add(usage, dayStart, monthStart, yearStart));

		return accumulators.entrySet().stream()
			.collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toResponse()));
	}

	private InternalRecommendationCardResponse toRecommendationCardResponse(
		CardRegistered card,
		CardProduct product,
		Long performanceAmount,
		RecommendationBenefitSources benefitSources,
		Map<BenefitUsageKey, InternalBenefitUsageResponse> usageByCardAndBenefit
	) {
		if (product == null) {
			throw new IllegalStateException("Card product not found for registered card.");
		}
		List<InternalRecommendationBenefitResponse> benefits = benefitSources.benefitsByProductId()
			.getOrDefault(card.getCardProductId(), List.of())
			.stream()
			.map(benefit -> toBenefitResponse(
				benefit,
				benefitSources.brandNamesByBenefitId().getOrDefault(benefit.getBenefitId(), List.of()),
				benefitSources.tiersByBenefitId().getOrDefault(benefit.getBenefitId(), List.of()),
				usageByCardAndBenefit.getOrDefault(
					new BenefitUsageKey(card.getCardId(), benefit.getBenefitId()),
					emptyUsage()
				)
			))
			.toList();

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

	private InternalRecommendationBenefitResponse toBenefitResponse(
		CardBenefit benefit,
		List<String> brandNames,
		List<CardBenefitTier> tiers,
		InternalBenefitUsageResponse usage
	) {
		return InternalRecommendationBenefitResponse.builder()
			.benefitId(benefit.getBenefitId())
			.serviceCategory(benefit.getServiceCategory().name())
			.benefitType(benefit.getBenefitType().name())
			.minAmount(benefit.getMinAmount())
			.timeStart(toTimeString(benefit.getTimeStart()))
			.timeEnd(toTimeString(benefit.getTimeEnd()))
			.dayCondition(benefit.getDayCondition().name())
			.benefitDesc(benefit.getBenefitDesc())
			.brandNames(brandNames)
			.usage(usage)
			.tiers(tiers.stream()
				.map(this::toTierResponse)
				.toList())
			.build();
	}

	private InternalRecommendationBenefitTierResponse toTierResponse(CardBenefitTier tier) {
		return InternalRecommendationBenefitTierResponse.builder()
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

	private InternalBenefitUsageResponse emptyUsage() {
		return InternalBenefitUsageResponse.builder()
			.dailyAmount(0L)
			.dailyCount(0L)
			.monthlyAmount(0L)
			.monthlyCount(0L)
			.yearlyAmount(0L)
			.yearlyCount(0L)
			.build();
	}

	private record RecommendationBenefitSources(
		Map<Long, List<CardBenefit>> benefitsByProductId,
		Map<Long, List<String>> brandNamesByBenefitId,
		Map<Long, List<CardBenefitTier>> tiersByBenefitId,
		List<Long> benefitIds
	) {
	}

	private record BenefitUsageKey(Long cardId, Long benefitId) {
	}

	private static class BenefitUsageAccumulator {

		private long dailyAmount;
		private long dailyCount;
		private long monthlyAmount;
		private long monthlyCount;
		private long yearlyAmount;
		private long yearlyCount;

		private void add(
			CardBenefitUsage usage,
			LocalDateTime dayStart,
			LocalDateTime monthStart,
			LocalDateTime yearStart
		) {
			long benefitAmount = usage.getBenefitAmount();
			LocalDateTime approvedAt = usage.getApprovedAt();

			if (!approvedAt.isBefore(yearStart)) {
				yearlyAmount += benefitAmount;
				yearlyCount++;
			}
			if (!approvedAt.isBefore(monthStart)) {
				monthlyAmount += benefitAmount;
				monthlyCount++;
			}
			if (!approvedAt.isBefore(dayStart)) {
				dailyAmount += benefitAmount;
				dailyCount++;
			}
		}

		private InternalBenefitUsageResponse toResponse() {
			return InternalBenefitUsageResponse.builder()
				.dailyAmount(dailyAmount)
				.dailyCount(dailyCount)
				.monthlyAmount(monthlyAmount)
				.monthlyCount(monthlyCount)
				.yearlyAmount(yearlyAmount)
				.yearlyCount(yearlyCount)
				.build();
		}
	}
}
