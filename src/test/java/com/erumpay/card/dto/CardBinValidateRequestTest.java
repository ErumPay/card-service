package com.erumpay.card.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CardBinValidateRequestTest {

	private static ValidatorFactory validatorFactory;
	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void closeValidatorFactory() {
		validatorFactory.close();
	}

	@Test
	void cardNumberMustBeSixteenDigits() {
		CardBinValidateRequest request = new CardBinValidateRequest("800012345678123");

		assertThat(validator.validate(request))
			.extracting(violation -> violation.getMessage())
			.contains("카드번호 형식 오류");
	}

	@Test
	void toStringDoesNotExposeCardNumber() {
		CardBinValidateRequest request = new CardBinValidateRequest("8000123456781234");

		assertThat(request.toString()).doesNotContain("8000123456781234");
	}
}
