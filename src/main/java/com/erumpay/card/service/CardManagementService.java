package com.erumpay.card.service;

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
import java.time.LocalDateTime;
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
public class CardManagementService {

	private static final String CARD_NOT_FOUND = "CARD_NOT_FOUND";
	private static final String CARD_NOT_ACTIVE = "CARD_NOT_ACTIVE";
	private static final String BILLING_KEY_NOT_FOUND = "BILLING_KEY_NOT_FOUND";
	private static final List<CardStatus> VISIBLE_CARD_STATUSES = List.of(
		CardStatus.ACTIVE,
		CardStatus.PAUSED,
		CardStatus.EXPIRED
	);

	private final CardRegisteredRepository cardRegisteredRepository;
	private final CardProductRepository cardProductRepository;
	private final Clock clock;

	// [be] 이준혁 260521 1602 | 삭제되지 않은 사용자 카드 목록을 조회하고 카드 상품 정보와 합쳐 응답 DTO로 변환한다.
	@Transactional(readOnly = true)
	public List<CardResponse> getCards(Long userId) {
		List<CardRegistered> cards = cardRegisteredRepository
			.findByUserIdAndStatusInOrderByDefaultCardDescCreatedAtDesc(userId, VISIBLE_CARD_STATUSES);
		Map<Long, CardProduct> products = findProductsById(cards.stream()
			.map(CardRegistered::getCardProductId)
			.toList());

		return cards.stream()
			.map(card -> toResponse(card, products.get(card.getCardProductId())))
			.toList();
	}

	// [be] 이준혁 260521 1602 | userId와 cardId가 함께 맞는 삭제되지 않은 카드만 상세 조회한다.
	@Transactional(readOnly = true)
	public CardResponse getCard(Long userId, Long cardId) {
		CardRegistered card = findOwnedVisibleCard(userId, cardId);
		CardProduct product = findProductById(card.getCardProductId());
		return toResponse(card, product);
	}

	// [be] 이준혁 260521 1602 | 카드 별칭을 수정한다. 공백만 있는 별칭은 null로 바꿔 별칭 제거로 처리한다.
	@Transactional
	public void updateAlias(Long userId, Long cardId, CardAliasUpdateRequest request) {
		CardRegistered card = findOwnedVisibleCard(userId, cardId);
		card.updateAlias(request.normalizedCardAlias());
	}

	// [be] 이준혁 260521 1602 | ACTIVE 카드만 주카드로 지정하고, 기존 주카드를 먼저 해제해 DB unique 충돌을 피한다.
	@Transactional
	public void setDefault(Long userId, Long cardId) {
		CardRegistered targetCard = findOwnedVisibleCard(userId, cardId);
		if (!targetCard.isActive()) {
			throw new CardNotActiveException();
		}
		if (targetCard.isDefaultCard()) {
			return;
		}

		cardRegisteredRepository.findByUserIdAndDefaultCardTrueAndStatus(userId, CardStatus.ACTIVE)
			.ifPresent(CardRegistered::unsetDefault);
		targetCard.markDefault();
	}

	// [be] 이준혁 260521 1602 | 카드는 물리 삭제하지 않고 DELETED 상태로 바꾼다. 주카드 삭제 시 다른 ACTIVE 카드를 대체 주카드로 지정한다.
	@Transactional
	public void deleteCard(Long userId, Long cardId) {
		CardRegistered card = findOwnedCard(userId, cardId);
		if (card.isDeleted()) {
			return;
		}
		if (card.isRegistering()) {
			throw new CardNotFoundException();
		}

		boolean defaultCard = card.isDefaultCard();
		card.delete(LocalDateTime.now(clock));

		if (defaultCard) {
			cardRegisteredRepository
				.findFirstByUserIdAndStatusAndCardIdNotOrderByCreatedAtAscCardIdAsc(
					userId,
					CardStatus.ACTIVE,
					cardId
				)
				.ifPresent(CardRegistered::markDefault);
		}
	}

	// [be] 이준혁 260521 1602 | 사용자에게 결제 가능한 ACTIVE 카드가 1장 이상 있는지 확인한다.
	@Transactional(readOnly = true)
	public PaymentAvailabilityResponse checkUserPaymentAvailability(Long userId) {
		if (!cardRegisteredRepository.existsByUserIdAndStatusIn(userId, VISIBLE_CARD_STATUSES)) {
			return PaymentAvailabilityResponse.unavailable(CARD_NOT_FOUND);
		}
		if (!cardRegisteredRepository.existsByUserIdAndStatus(userId, CardStatus.ACTIVE)) {
			return PaymentAvailabilityResponse.unavailable(CARD_NOT_ACTIVE);
		}
		if (!cardRegisteredRepository.existsPaymentAvailableCard(userId, CardStatus.ACTIVE)) {
			return PaymentAvailabilityResponse.unavailable(BILLING_KEY_NOT_FOUND);
		}
		return PaymentAvailabilityResponse.available();
	}

	// [be] 이준혁 260521 1602 | 특정 카드 1장이 결제 가능한 ACTIVE 카드이고 billing key를 갖는지 확인한다.
	@Transactional(readOnly = true)
	public PaymentAvailabilityResponse checkCardPaymentAvailability(Long userId, Long cardId) {
		CardRegistered card = findOwnedVisibleCard(userId, cardId);
		if (!card.isActive()) {
			return PaymentAvailabilityResponse.unavailable(CARD_NOT_ACTIVE);
		}
		if (!card.hasBillingKey()) {
			return PaymentAvailabilityResponse.unavailable(BILLING_KEY_NOT_FOUND);
		}
		return PaymentAvailabilityResponse.available();
	}

	private CardRegistered findOwnedVisibleCard(Long userId, Long cardId) {
		return cardRegisteredRepository.findByCardIdAndUserIdAndStatusIn(cardId, userId, VISIBLE_CARD_STATUSES)
			.orElseThrow(CardNotFoundException::new);
	}

	private CardRegistered findOwnedCard(Long userId, Long cardId) {
		return cardRegisteredRepository.findByCardIdAndUserId(cardId, userId)
			.orElseThrow(CardNotFoundException::new);
	}

	private Map<Long, CardProduct> findProductsById(Collection<Long> cardProductIds) {
		return cardProductRepository.findAllById(cardProductIds).stream()
			.collect(Collectors.toMap(CardProduct::getCardProductId, Function.identity()));
	}

	private CardProduct findProductById(Long cardProductId) {
		return cardProductRepository.findById(cardProductId)
			.orElseThrow(() -> new IllegalStateException("Card product not found for registered card."));
	}

	private CardResponse toResponse(CardRegistered card, CardProduct product) {
		if (product == null) {
			throw new IllegalStateException("Card product not found for registered card.");
		}

		return CardResponse.builder()
			.cardId(card.getCardId())
			.cardProductId(card.getCardProductId())
			.cardCompany(product.getCardCompany())
			.cardName(product.getCardName())
			.maskedNumber(card.getMaskedNumber())
			.cardAlias(card.getCardAlias())
			.expiryYm(card.getExpiryYm())
			.isDefault(card.isDefaultCard())
			.status(card.getStatus())
			.build();
	}
}
