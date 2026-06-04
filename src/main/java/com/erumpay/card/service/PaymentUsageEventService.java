package com.erumpay.card.service;

import com.erumpay.card.domain.entity.CardBenefitUsage;
import com.erumpay.card.domain.entity.CardPerformance;
import com.erumpay.card.domain.entity.CardRegistered;
import com.erumpay.card.domain.enums.PaymentUsageEventType;
import com.erumpay.card.dto.PaymentUsageEventRequest;
import com.erumpay.card.dto.PaymentUsageEventRequest.AppliedBenefitRequest;
import com.erumpay.card.dto.PaymentUsageEventRequest.PaymentUsageCardRequest;
import com.erumpay.card.dto.PaymentUsageEventResponse;
import com.erumpay.card.exception.CardNotActiveException;
import com.erumpay.card.exception.CardNotFoundException;
import com.erumpay.card.exception.InvalidPaymentUsageEventRequestException;
import com.erumpay.card.repository.CardBenefitRepository;
import com.erumpay.card.repository.CardBenefitTierRepository;
import com.erumpay.card.repository.CardBenefitUsageRepository;
import com.erumpay.card.repository.CardPerformanceRepository;
import com.erumpay.card.repository.CardRegisteredRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentUsageEventService {

	private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

	private final CardRegisteredRepository cardRegisteredRepository;
	private final CardPerformanceRepository cardPerformanceRepository;
	private final CardBenefitRepository cardBenefitRepository;
	private final CardBenefitTierRepository cardBenefitTierRepository;
	private final CardBenefitUsageRepository cardBenefitUsageRepository;

	@Transactional
	public PaymentUsageEventResponse apply(Long userId, PaymentUsageEventRequest request) {
		validateRequest(request);
		Map<Long, CardRegistered> cardsById = findOwnedCards(userId, request);

		if (request.eventType() == PaymentUsageEventType.APPROVED) {
			applyApproved(userId, request, cardsById);
		} else {
			applyCanceled(userId, request, cardsById);
		}

		return PaymentUsageEventResponse.builder()
			.paymentId(request.paymentId())
			.eventType(request.eventType())
			.applied(true)
			.appliedCardCount(request.cards().size())
			.reason(null)
			.build();
	}

	private void validateRequest(PaymentUsageEventRequest request) {
		if (request == null || request.paymentId() == null || request.eventType() == null
			|| request.occurredAt() == null || request.cards() == null || request.cards().isEmpty()) {
			throw new InvalidPaymentUsageEventRequestException("payment usage event request is invalid");
		}

		Set<Long> paymentCardIds = new HashSet<>();
		Set<Long> cardIds = new HashSet<>();
		for (PaymentUsageCardRequest cardRequest : request.cards()) {
			if (cardRequest == null) {
				throw new InvalidPaymentUsageEventRequestException("card request is required");
			}
			if (cardRequest.paymentCardId() == null || !paymentCardIds.add(cardRequest.paymentCardId())) {
				throw new InvalidPaymentUsageEventRequestException("paymentCardId must be unique");
			}
			if (cardRequest.cardId() == null || !cardIds.add(cardRequest.cardId())) {
				throw new InvalidPaymentUsageEventRequestException("cardId must be unique");
			}
			if (cardRequest.approvedAmount() == null || cardRequest.approvedAmount() <= 0) {
				throw new InvalidPaymentUsageEventRequestException("approvedAmount must be positive");
			}
			if (request.eventType() == PaymentUsageEventType.CANCELED && cardRequest.approvedAt() == null) {
				throw new InvalidPaymentUsageEventRequestException("approvedAt is required for canceled event");
			}
			validateBenefitRequest(cardRequest.appliedBenefit());
		}
	}

	private void validateBenefitRequest(AppliedBenefitRequest benefitRequest) {
		if (benefitRequest == null) {
			return;
		}
		if (benefitRequest.benefitId() == null || benefitRequest.tierId() == null
			|| benefitRequest.benefitAmount() == null || benefitRequest.benefitAmount() <= 0) {
			throw new InvalidPaymentUsageEventRequestException("appliedBenefit request is invalid");
		}
	}

	private Map<Long, CardRegistered> findOwnedCards(Long userId, PaymentUsageEventRequest request) {
		List<Long> cardIds = request.cards().stream()
			.map(PaymentUsageCardRequest::cardId)
			.toList();
		Map<Long, CardRegistered> cardsById = cardRegisteredRepository
			.findByUserIdAndCardIdIn(userId, cardIds)
			.stream()
			.collect(Collectors.toMap(CardRegistered::getCardId, Function.identity()));

		for (Long cardId : cardIds) {
			CardRegistered card = cardsById.get(cardId);
			if (card == null) {
				throw new CardNotFoundException();
			}
			if (request.eventType() == PaymentUsageEventType.APPROVED && !card.isActive()) {
				throw new CardNotActiveException();
			}
		}
		return cardsById;
	}

	private void applyApproved(
		Long userId,
		PaymentUsageEventRequest request,
		Map<Long, CardRegistered> cardsById
	) {
		for (PaymentUsageCardRequest cardRequest : request.cards()) {
			CardRegistered card = cardsById.get(cardRequest.cardId());
			validateBenefit(card, cardRequest.appliedBenefit());
			if (cardRequest.appliedBenefit() != null) {
				saveApprovedBenefitUsage(userId, request, cardRequest);
			}
			increasePerformance(userId, card.getCardId(), yearMonth(request.occurredAt()), cardRequest.approvedAmount());
		}
	}

	private void applyCanceled(
		Long userId,
		PaymentUsageEventRequest request,
		Map<Long, CardRegistered> cardsById
	) {
		for (PaymentUsageCardRequest cardRequest : request.cards()) {
			CardRegistered card = cardsById.get(cardRequest.cardId());
			if (!cancelBenefitUsageIfNeeded(request, cardRequest)) {
				continue;
			}
			decreasePerformance(
				userId,
				card.getCardId(),
				yearMonth(cardRequest.approvedAt()),
				cardRequest.approvedAmount(),
				request.paymentId()
			);
		}
	}

	private void validateBenefit(CardRegistered card, AppliedBenefitRequest benefitRequest) {
		if (benefitRequest == null) {
			return;
		}
		if (!cardBenefitRepository.existsByBenefitIdAndCardProductId(
			benefitRequest.benefitId(),
			card.getCardProductId()
		)) {
			throw new InvalidPaymentUsageEventRequestException("benefit does not belong to card product");
		}
		if (!cardBenefitTierRepository.existsByTierIdAndBenefitId(
			benefitRequest.tierId(),
			benefitRequest.benefitId()
		)) {
			throw new InvalidPaymentUsageEventRequestException("tier does not belong to benefit");
		}
	}

	private void saveApprovedBenefitUsage(
		Long userId,
		PaymentUsageEventRequest request,
		PaymentUsageCardRequest cardRequest
	) {
		AppliedBenefitRequest benefit = cardRequest.appliedBenefit();
		cardBenefitUsageRepository.save(CardBenefitUsage.approved(
			request.paymentId(),
			userId,
			cardRequest.cardId(),
			benefit.benefitId(),
			benefit.tierId(),
			toIntegerAmount(cardRequest.approvedAmount(), "approvedAmount"),
			toIntegerAmount(benefit.benefitAmount(), "benefitAmount"),
			request.occurredAt()
		));
	}

	private boolean cancelBenefitUsageIfNeeded(PaymentUsageEventRequest request, PaymentUsageCardRequest cardRequest) {
		List<CardBenefitUsage> usages = cardBenefitUsageRepository.findByPaymentIdAndCardId(
			request.paymentId(),
			cardRequest.cardId()
		);
		if (usages.isEmpty()) {
			return true;
		}
		if (usages.size() > 1) {
			throw new InvalidPaymentUsageEventRequestException("multiple benefit usages found for payment card");
		}

		CardBenefitUsage usage = usages.getFirst();
		if (!usage.cancel(request.occurredAt())) {
			log.warn(
				"Payment benefit usage already canceled. paymentId={}, cardId={}, benefitId={}, tierId={}",
				request.paymentId(),
				cardRequest.cardId(),
				usage.getBenefitId(),
				usage.getTierId()
			);
			return false;
		}
		return true;
	}

	private void increasePerformance(Long userId, Long cardId, String yearMonth, Long amount) {
		cardPerformanceRepository.findByCardIdAndUserIdAndYearMonth(cardId, userId, yearMonth)
			.ifPresentOrElse(
				performance -> performance.increase(amount),
				() -> cardPerformanceRepository.save(CardPerformance.create(userId, cardId, yearMonth, amount))
			);
	}

	private void decreasePerformance(Long userId, Long cardId, String yearMonth, Long amount, Long paymentId) {
		cardPerformanceRepository.findByCardIdAndUserIdAndYearMonth(cardId, userId, yearMonth)
			.ifPresentOrElse(
				performance -> {
					boolean clamped = performance.decreaseWithFloor(amount);
					if (clamped) {
						log.warn(
							"Card performance decrement clamped to zero. userId={}, cardId={}, yearMonth={}, paymentId={}",
							userId,
							cardId,
							yearMonth,
							paymentId
						);
					}
				},
				() -> log.warn(
					"Card performance row not found during cancel. userId={}, cardId={}, yearMonth={}, paymentId={}",
					userId,
					cardId,
					yearMonth,
					paymentId
				)
			);
	}

	private int toIntegerAmount(Long amount, String fieldName) {
		if (amount > Integer.MAX_VALUE) {
			throw new InvalidPaymentUsageEventRequestException(fieldName + " exceeds storage limit");
		}
		return amount.intValue();
	}

	private String yearMonth(LocalDateTime dateTime) {
		return dateTime.format(YEAR_MONTH_FORMATTER);
	}
}
