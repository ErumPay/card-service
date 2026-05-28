package com.erumpay.card.controller;

import com.erumpay.card.dto.CardAliasUpdateRequest;
import com.erumpay.card.dto.CardBinValidateRequest;
import com.erumpay.card.dto.CardBinValidateResponse;
import com.erumpay.card.dto.CardBenefitResponse;
import com.erumpay.card.dto.CardPerformanceResponse;
import com.erumpay.card.dto.CardRegisterRequest;
import com.erumpay.card.dto.CardRegisterResponse;
import com.erumpay.card.dto.CardResponse;
import com.erumpay.card.dto.PaymentAvailabilityResponse;
import com.erumpay.card.service.CardBinValidationService;
import com.erumpay.card.service.CardManagementService;
import com.erumpay.card.service.CardPerformanceBenefitService;
import com.erumpay.card.service.CardRegistrationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cards")
public class CardController {

	private final CardRegistrationService cardRegistrationService;
	private final CardManagementService cardManagementService;
	private final CardBinValidationService cardBinValidationService;
	private final CardPerformanceBenefitService cardPerformanceBenefitService;

	@PostMapping
	public ResponseEntity<CardRegisterResponse> register(@Valid @RequestBody CardRegisterRequest request) {
		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(cardRegistrationService.register(request));
	}

	@PostMapping("/bin/validate")
	public ResponseEntity<CardBinValidateResponse> validateBin(@Valid @RequestBody CardBinValidateRequest request) {
		return ResponseEntity.ok(cardBinValidationService.validate(request));
	}

	@GetMapping
	public ResponseEntity<List<CardResponse>> getCards(@RequestParam Long userId) {
		return ResponseEntity.ok(cardManagementService.getCards(userId));
	}

	@GetMapping("/{cardId}")
	public ResponseEntity<CardResponse> getCard(@PathVariable Long cardId, @RequestParam Long userId) {
		return ResponseEntity.ok(cardManagementService.getCard(userId, cardId));
	}

	@GetMapping("/{cardId}/performance")
	public ResponseEntity<CardPerformanceResponse> getPerformance(
		@PathVariable Long cardId,
		@RequestParam Long userId,
		@RequestParam String yearMonth
	) {
		return ResponseEntity.ok(cardPerformanceBenefitService.getPerformance(userId, cardId, yearMonth));
	}

	@GetMapping("/{cardId}/benefits")
	public ResponseEntity<List<CardBenefitResponse>> getBenefits(
		@PathVariable Long cardId,
		@RequestParam Long userId
	) {
		return ResponseEntity.ok(cardPerformanceBenefitService.getBenefits(userId, cardId));
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
