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
import com.erumpay.card.exception.AuthorizationRequiredException;
import com.erumpay.card.exception.InvalidRequestException;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cards")
public class CardController {

	private static final String USER_ID_HEADER = "X-User-Id";

	private final CardRegistrationService cardRegistrationService;
	private final CardManagementService cardManagementService;
	private final CardBinValidationService cardBinValidationService;
	private final CardPerformanceBenefitService cardPerformanceBenefitService;

	@PostMapping
	public ResponseEntity<CardRegisterResponse> register(
		@RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader,
		@Valid @RequestBody CardRegisterRequest request
	) {
		return ResponseEntity
			.status(HttpStatus.CREATED)
			.body(cardRegistrationService.register(requireUserId(userIdHeader), request));
	}

	@PostMapping("/bin/validate")
	public ResponseEntity<CardBinValidateResponse> validateBin(@Valid @RequestBody CardBinValidateRequest request) {
		return ResponseEntity.ok(cardBinValidationService.validate(request));
	}

	@GetMapping
	public ResponseEntity<List<CardResponse>> getCards(
		@RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader
	) {
		return ResponseEntity.ok(cardManagementService.getCards(requireUserId(userIdHeader)));
	}

	@GetMapping("/{cardId}")
	public ResponseEntity<CardResponse> getCard(
		@PathVariable Long cardId,
		@RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader
	) {
		return ResponseEntity.ok(cardManagementService.getCard(requireUserId(userIdHeader), cardId));
	}

	@GetMapping("/{cardId}/performance")
	public ResponseEntity<CardPerformanceResponse> getPerformance(
		@PathVariable Long cardId,
		@RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader,
		@RequestParam String yearMonth
	) {
		return ResponseEntity.ok(cardPerformanceBenefitService.getPerformance(
			requireUserId(userIdHeader),
			cardId,
			yearMonth
		));
	}

	@GetMapping("/{cardId}/benefits")
	public ResponseEntity<List<CardBenefitResponse>> getBenefits(
		@PathVariable Long cardId,
		@RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader
	) {
		return ResponseEntity.ok(cardPerformanceBenefitService.getBenefits(requireUserId(userIdHeader), cardId));
	}

	@PatchMapping("/{cardId}/alias")
	public ResponseEntity<Void> updateAlias(
		@PathVariable Long cardId,
		@RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader,
		@Valid @RequestBody CardAliasUpdateRequest request
	) {
		cardManagementService.updateAlias(requireUserId(userIdHeader), cardId, request);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{cardId}/default")
	public ResponseEntity<Void> setDefault(
		@PathVariable Long cardId,
		@RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader
	) {
		cardManagementService.setDefault(requireUserId(userIdHeader), cardId);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{cardId}")
	public ResponseEntity<Void> deleteCard(
		@PathVariable Long cardId,
		@RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader
	) {
		cardManagementService.deleteCard(requireUserId(userIdHeader), cardId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/payment-availability")
	public ResponseEntity<PaymentAvailabilityResponse> checkUserPaymentAvailability(
		@RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader
	) {
		return ResponseEntity.ok(cardManagementService.checkUserPaymentAvailability(
			requireUserId(userIdHeader)
		));
	}

	@GetMapping("/{cardId}/payment-availability")
	public ResponseEntity<PaymentAvailabilityResponse> checkCardPaymentAvailability(
		@PathVariable Long cardId,
		@RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader
	) {
		return ResponseEntity.ok(cardManagementService.checkCardPaymentAvailability(
			requireUserId(userIdHeader),
			cardId
		));
	}

	private Long requireUserId(String userIdHeader) {
		if (userIdHeader == null || userIdHeader.isBlank()) {
			throw new AuthorizationRequiredException();
		}
		try {
			Long userId = Long.valueOf(userIdHeader.trim());
			if (userId <= 0) {
				throw new InvalidRequestException();
			}
			return userId;
		} catch (NumberFormatException exception) {
			throw new InvalidRequestException(exception);
		}
	}
}
