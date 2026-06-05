package com.erumpay.card.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders
			.standaloneSetup(new TestController())
			.setControllerAdvice(new GlobalExceptionHandler())
			.setValidator(validator)
			.build();
	}

	@Test
	void validationFailureReturnsTeamErrorResponseWithDetailsAndCorrelationId() throws Exception {
		mockMvc.perform(post("/test/validate")
				.contentType(MediaType.APPLICATION_JSON)
				.header("X-Correlation-Id", "test-correlation-id")
				.content("{}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.error").value("BAD_REQUEST"))
			.andExpect(jsonPath("$.code").value("CARD-REQ-001"))
			.andExpect(jsonPath("$.reason").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
			.andExpect(jsonPath("$.details[0].field").value("name"))
			.andExpect(jsonPath("$.details[0].message").value("name is required"))
			.andExpect(jsonPath("$.correlationId").value("test-correlation-id"))
			.andExpect(jsonPath("$.path").value("/test/validate"));
	}

	@Test
	void missingRequestParameterReturnsInvalidRequestWithNullCorrelationId() throws Exception {
		mockMvc.perform(get("/test/missing-param"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("CARD-REQ-001"))
			.andExpect(jsonPath("$.reason").value("INVALID_REQUEST"))
			.andExpect(jsonPath("$.details[0].field").value("userId"))
			.andExpect(jsonPath("$.correlationId").doesNotExist());
	}

	@Test
	void typeMismatchReturnsInvalidRequestDetail() throws Exception {
		mockMvc.perform(get("/test/type-mismatch").param("userId", "abc"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("CARD-REQ-001"))
			.andExpect(jsonPath("$.details[0].field").value("userId"))
			.andExpect(jsonPath("$.details[0].message").value("요청 파라미터 형식이 올바르지 않습니다."));
	}

	@Test
	void missingPathVariableReturnsInvalidRequestDetail() throws Exception {
		mockMvc.perform(get("/test/missing-path"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("CARD-REQ-001"))
			.andExpect(jsonPath("$.details[0].field").value("cardId"))
			.andExpect(jsonPath("$.details[0].message").value("필수 경로 변수입니다."));
	}

	@Test
	void cardServiceExceptionReturnsErrorCodeResponse() throws Exception {
		mockMvc.perform(get("/test/card-not-found"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.error").value("NOT_FOUND"))
			.andExpect(jsonPath("$.code").value("CARD-CARD-201"))
			.andExpect(jsonPath("$.reason").value("CARD_NOT_FOUND"))
			.andExpect(jsonPath("$.message").value("카드를 찾을 수 없습니다."))
			.andExpect(jsonPath("$.path").value("/test/card-not-found"));
	}

	@Test
	void genericExceptionReturnsInternalServerError() throws Exception {
		mockMvc.perform(get("/test/generic"))
			.andExpect(status().isInternalServerError())
			.andExpect(jsonPath("$.status").value(500))
			.andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
			.andExpect(jsonPath("$.code").value("CARD-SYS-900"))
			.andExpect(jsonPath("$.reason").value("INTERNAL_SERVER_ERROR"))
			.andExpect(jsonPath("$.message").value("알 수 없는 내부 오류가 발생했습니다."))
			.andExpect(jsonPath("$.path").value("/test/generic"));
	}

	@RestController
	private static class TestController {

		@PostMapping("/test/validate")
		void validate(@Valid @RequestBody TestRequest request) {
		}

		@GetMapping("/test/missing-param")
		void missingParam(@RequestParam Long userId) {
		}

		@GetMapping("/test/type-mismatch")
		void typeMismatch(@RequestParam Long userId) {
		}

		@GetMapping("/test/missing-path")
		void missingPath(@PathVariable("cardId") Long cardId) {
		}

		@GetMapping("/test/card-not-found")
		void cardNotFound() {
			throw new CardNotFoundException();
		}

		@GetMapping("/test/generic")
		void generic() {
			throw new RuntimeException("boom");
		}
	}

	private record TestRequest(
		@NotBlank(message = "name is required")
		String name
	) {
	}
}
