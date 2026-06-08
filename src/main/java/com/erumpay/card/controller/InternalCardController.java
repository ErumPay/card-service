package com.erumpay.card.controller;

import com.erumpay.card.dto.InternalBillingKeyResponse;
import com.erumpay.card.dto.InternalBillingKeysRequest;
import com.erumpay.card.dto.InternalBillingKeysResponse;
import com.erumpay.card.dto.InternalDeactivateCardsResponse;
import com.erumpay.card.dto.InternalDefaultCardResponse;
import com.erumpay.card.dto.InternalRecommendationSourceResponse;
import com.erumpay.card.dto.PaymentUsageEventRequest;
import com.erumpay.card.dto.PaymentUsageEventResponse;
import com.erumpay.card.service.InternalCardService;
import com.erumpay.card.service.PaymentUsageEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/cards")
public class InternalCardController {

	private final InternalCardService internalCardService;
	private final PaymentUsageEventService paymentUsageEventService;

	@GetMapping("/{cardId}/billing-key")
	public ResponseEntity<InternalBillingKeyResponse> getBillingKey(
		@PathVariable Long cardId,
		@RequestParam Long userId
	) {
		return ResponseEntity.ok(internalCardService.getBillingKey(userId, cardId));
	}

	@PostMapping("/users/{userId}/billing-keys")
	public ResponseEntity<InternalBillingKeysResponse> getBillingKeys(
		@PathVariable Long userId,
		@RequestBody @Valid InternalBillingKeysRequest request
	) {
		return ResponseEntity.ok(internalCardService.getBillingKeys(userId, request.cardIds()));
	}

	@GetMapping("/users/{userId}/default-card")
	public ResponseEntity<InternalDefaultCardResponse> getDefaultCard(@PathVariable Long userId) {
		return ResponseEntity.ok(internalCardService.getDefaultCard(userId));
	}

	@GetMapping("/users/{userId}/recommendation-source")
	public ResponseEntity<InternalRecommendationSourceResponse> getRecommendationSource(
		@PathVariable Long userId,
		@RequestParam String yearMonth
	) {
		return ResponseEntity.ok(internalCardService.getRecommendationSource(userId, yearMonth));
	}

	@PostMapping("/users/{userId}/deactivate-all")
	public ResponseEntity<InternalDeactivateCardsResponse> deactivateAll(@PathVariable Long userId) {
		return ResponseEntity.ok(internalCardService.deactivateAll(userId));
	}

	@PostMapping("/users/{userId}/payment-usage-events")
	public ResponseEntity<PaymentUsageEventResponse> applyPaymentUsageEvent(
		@PathVariable Long userId,
		@RequestBody @Valid PaymentUsageEventRequest request
	) {
		return ResponseEntity.ok(paymentUsageEventService.apply(userId, request));
	}
}
