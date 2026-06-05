package com.erumpay.card.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.erumpay.card.domain.enums.CardStatus;
import com.erumpay.card.exception.GlobalExceptionHandler;
import com.erumpay.card.dto.CardRegisterResponse;
import com.erumpay.card.dto.CardResponse;
import com.erumpay.card.service.CardBinValidationService;
import com.erumpay.card.service.CardManagementService;
import com.erumpay.card.service.CardPerformanceBenefitService;
import com.erumpay.card.service.CardRegistrationService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class CardControllerTest {

	private CardRegistrationService cardRegistrationService;
	private CardManagementService cardManagementService;
	private CardBinValidationService cardBinValidationService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		cardRegistrationService = mock(CardRegistrationService.class);
		cardManagementService = mock(CardManagementService.class);
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
	void registerUsesXUserIdHeaderAndDoesNotRequireUserIdInBody() throws Exception {
		when(cardRegistrationService.register(eq(2L), any()))
			.thenReturn(CardRegisterResponse.builder()
				.cardId(100L)
				.cardProductId(10L)
				.cardCompany("LotteCard")
				.cardName("LOCA 365")
				.maskedNumber("8000-****-****-1234")
				.expiryYm("202812")
				.isDefault(true)
				.status(CardStatus.ACTIVE)
				.build());

		mockMvc.perform(post("/api/v1/cards")
				.contentType(MediaType.APPLICATION_JSON)
				.header("X-User-Id", "2")
				.content("""
					{
					  "cardNumber": "8000001234567890",
					  "expiryYm": "202812",
					  "cvc": "123",
					  "cardPassword2": "12",
					  "cardAlias": "생활비 카드",
					  "isDefault": true
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.cardId").value(100L))
			.andExpect(jsonPath("$.status").value("ACTIVE"));

		verify(cardRegistrationService).register(eq(2L), any());
	}

	@Test
	void registerReturnsUnauthorizedWhenXUserIdIsMissing() throws Exception {
		mockMvc.perform(post("/api/v1/cards")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "cardNumber": "8000001234567890",
					  "expiryYm": "202812",
					  "cvc": "123",
					  "cardPassword2": "12"
					}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.code").value("CARD-AUTH-100"))
			.andExpect(jsonPath("$.reason").value("AUTHORIZATION_REQUIRED"));

		verify(cardRegistrationService, never()).register(any(), any());
	}

	@Test
	void registerReturnsBadRequestWhenXUserIdIsNotPositiveNumber() throws Exception {
		mockMvc.perform(post("/api/v1/cards")
				.contentType(MediaType.APPLICATION_JSON)
				.header("X-User-Id", "abc")
				.content("""
					{
					  "cardNumber": "8000001234567890",
					  "expiryYm": "202812",
					  "cvc": "123",
					  "cardPassword2": "12"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.code").value("CARD-REQ-001"))
			.andExpect(jsonPath("$.reason").value("INVALID_REQUEST"));

		mockMvc.perform(get("/api/v1/cards")
				.header("X-User-Id", "0"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("CARD-REQ-001"))
			.andExpect(jsonPath("$.reason").value("INVALID_REQUEST"));
	}

	@Test
	void getCardsUsesXUserIdHeader() throws Exception {
		when(cardManagementService.getCards(2L))
			.thenReturn(List.of(CardResponse.builder()
				.cardId(100L)
				.cardProductId(10L)
				.cardCompany("LotteCard")
				.cardName("LOCA 365")
				.maskedNumber("8000-****-****-1234")
				.expiryYm("202812")
				.isDefault(true)
				.status(CardStatus.ACTIVE)
				.build()));

		mockMvc.perform(get("/api/v1/cards")
				.header("X-User-Id", "2"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].cardId").value(100L));

		verify(cardManagementService).getCards(2L);
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
