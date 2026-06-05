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
import com.erumpay.card.service.CardPerformanceBenefitService;
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
		CardPerformanceBenefitService cardPerformanceBenefitService = mock(CardPerformanceBenefitService.class);

		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders
			.standaloneSetup(new CardController(
				cardRegistrationService,
				cardManagementService,
				cardBinValidationService,
				cardPerformanceBenefitService
			))
			.setControllerAdvice(new GlobalExceptionHandler())
			.setValidator(validator)
			.build();
	}

	@Test
	void validateBinReturnsBadRequestWhenCardNumberIsNotSixteenDigits() throws Exception {
		mockMvc.perform(post("/api/v1/cards/bin/validate")
				.contentType(MediaType.APPLICATION_JSON)
				.header("X-Correlation-Id", "test-correlation-id")
				.content("{\"cardNumber\":\"800012345678123\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.error").value("BAD_REQUEST"))
			.andExpect(jsonPath("$.code").value("CARD-REQ-001"))
			.andExpect(jsonPath("$.reason").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
			.andExpect(jsonPath("$.details[0].field").value("cardNumber"))
			.andExpect(jsonPath("$.details[0].message").value("카드번호 형식 오류"))
			.andExpect(jsonPath("$.correlationId").value("test-correlation-id"))
			.andExpect(jsonPath("$.path").value("/api/v1/cards/bin/validate"));

		verify(cardBinValidationService, never()).validate(any());
	}
}
