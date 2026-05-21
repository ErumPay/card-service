package com.erumpay.card.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erumpay.card.exception.GlobalExceptionHandler;
import com.erumpay.card.service.CardBinValidationService;
import com.erumpay.card.service.CardManagementService;
import com.erumpay.card.service.CardRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class CardControllerTest {

	private CardBinValidationService cardBinValidationService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		CardRegistrationService cardRegistrationService = mock(CardRegistrationService.class);
		CardManagementService cardManagementService = mock(CardManagementService.class);
		cardBinValidationService = mock(CardBinValidationService.class);

		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders
			.standaloneSetup(new CardController(
				cardRegistrationService,
				cardManagementService,
				cardBinValidationService
			))
			.setControllerAdvice(new GlobalExceptionHandler())
			.setValidator(validator)
			.build();
	}

	@Test
	void validateBinReturnsBadRequestWhenCardNumberIsNotSixteenDigits() throws Exception {
		mockMvc.perform(post("/api/v1/cards/bin/validate")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"cardNumber\":\"800012345678123\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.message").value("카드번호 형식 오류"));

		verify(cardBinValidationService, never()).validate(any());
	}
}
