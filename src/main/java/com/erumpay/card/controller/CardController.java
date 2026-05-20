package com.erumpay.card.controller;

import com.erumpay.card.dto.CardRegisterRequest;
import com.erumpay.card.service.CardRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cards")
public class CardController {

	private final CardRegistrationService cardRegistrationService;

	@PostMapping
	public ResponseEntity<Void> register(@Valid @RequestBody CardRegisterRequest request) {
		cardRegistrationService.register(request);
		return ResponseEntity.noContent().build();
	}
}
