package com.erumpay.card.controller;

import com.erumpay.card.dto.InternalBillingKeyResponse;
import com.erumpay.card.dto.InternalDeactivateCardsResponse;
import com.erumpay.card.dto.InternalDefaultCardResponse;
import com.erumpay.card.dto.InternalRecommendationSourceResponse;
import com.erumpay.card.service.InternalCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/cards")
public class InternalCardController {

	private final InternalCardService internalCardService;

	@GetMapping("/{cardId}/billing-key")
	public ResponseEntity<InternalBillingKeyResponse> getBillingKey(
		@PathVariable Long cardId,
		@RequestParam Long userId
	) {
		return ResponseEntity.ok(internalCardService.getBillingKey(userId, cardId));
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
}
