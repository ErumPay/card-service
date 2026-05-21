package com.erumpay.card.controller;

import com.erumpay.card.dto.CardRegisterRequest;
import com.erumpay.card.dto.CardResponse;
import com.erumpay.card.dto.CardAliasUpdateRequest;
import com.erumpay.card.dto.PaymentAvailabilityResponse;
import com.erumpay.card.service.CardManagementService;
import com.erumpay.card.service.CardRegistrationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cards")
public class CardController {

	private final CardRegistrationService cardRegistrationService;
	private final CardManagementService cardManagementService;

	@PostMapping
	public ResponseEntity<Void> register(@Valid @RequestBody CardRegisterRequest request) {
		cardRegistrationService.register(request);
		return ResponseEntity.noContent().build();
	}

	@GetMapping
	public ResponseEntity<List<CardResponse>> getCards(@RequestParam Long userId) {
		return ResponseEntity.ok(cardManagementService.getCards(userId));
	}

	@GetMapping("/{cardId}")
	public ResponseEntity<CardResponse> getCard(@PathVariable Long cardId, @RequestParam Long userId) {
		return ResponseEntity.ok(cardManagementService.getCard(userId, cardId));
	}

	@PatchMapping("/{cardId}/alias")
	public ResponseEntity<Void> updateAlias(
		@PathVariable Long cardId,
		@RequestParam Long userId,
		@Valid @RequestBody CardAliasUpdateRequest request
	) {
		cardManagementService.updateAlias(userId, cardId, request);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{cardId}/default")
	public ResponseEntity<Void> setDefault(@PathVariable Long cardId, @RequestParam Long userId) {
		cardManagementService.setDefault(userId, cardId);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{cardId}")
	public ResponseEntity<Void> deleteCard(@PathVariable Long cardId, @RequestParam Long userId) {
		cardManagementService.deleteCard(userId, cardId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/payment-availability")
	public ResponseEntity<PaymentAvailabilityResponse> checkUserPaymentAvailability(@RequestParam Long userId) {
		return ResponseEntity.ok(cardManagementService.checkUserPaymentAvailability(userId));
	}

	@GetMapping("/{cardId}/payment-availability")
	public ResponseEntity<PaymentAvailabilityResponse> checkCardPaymentAvailability(
		@PathVariable Long cardId,
		@RequestParam Long userId
	) {
		return ResponseEntity.ok(cardManagementService.checkCardPaymentAvailability(userId, cardId));
	}
}
