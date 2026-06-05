package com.erumpay.card.exception;

import com.erumpay.card.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

	@ExceptionHandler(CardServiceException.class)
	public ResponseEntity<ErrorResponse> handleCardServiceException(
		CardServiceException exception,
		HttpServletRequest request
	) {
		ErrorCode errorCode = exception.getErrorCode();
		log.warn(
			"Card service exception handled. status={}, code={}, reason={}, message={}, detail={}",
			errorCode.getStatus(),
			errorCode.getCode(),
			errorCode.getReason(),
			exception.getMessage(),
			detail(exception)
		);
		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ErrorResponse.of(
				errorCode,
				exception.getMessage(),
				request.getRequestURI(),
				List.of(),
				correlationId(request)
			));
	}

	@ExceptionHandler({
		MethodArgumentNotValidException.class,
		ConstraintViolationException.class,
		HandlerMethodValidationException.class,
		MethodArgumentTypeMismatchException.class,
		MissingServletRequestParameterException.class,
		MissingPathVariableException.class,
		HttpMessageNotReadableException.class
	})
	public ResponseEntity<ErrorResponse> handleInvalidRequest(
		Exception exception,
		HttpServletRequest request
	) {
		ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
		log.warn("Invalid request. exceptionType={}", exception.getClass().getSimpleName());

		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ErrorResponse.of(
				errorCode,
				errorCode.getMessage(),
				request.getRequestURI(),
				validationDetails(exception),
				correlationId(request)
			));
	}

	@ExceptionHandler(Throwable.class)
	public ResponseEntity<ErrorResponse> handleThrowable(Throwable exception, HttpServletRequest request) {
		ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
		log.error("Unhandled exception occurred in GlobalExceptionHandler.", exception);

		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ErrorResponse.of(
				errorCode,
				errorCode.getMessage(),
				request.getRequestURI(),
				List.of(),
				correlationId(request)
			));
	}

	private List<ErrorResponse.ErrorDetail> validationDetails(Exception exception) {
		if (exception instanceof MethodArgumentNotValidException methodArgumentNotValidException) {
			List<ErrorResponse.ErrorDetail> details = new ArrayList<>();
			methodArgumentNotValidException.getBindingResult().getFieldErrors()
				.forEach(error -> details.add(new ErrorResponse.ErrorDetail(
					error.getField(),
					error.getDefaultMessage()
				)));
			methodArgumentNotValidException.getBindingResult().getGlobalErrors()
				.forEach(error -> details.add(new ErrorResponse.ErrorDetail(
					error.getObjectName(),
					error.getDefaultMessage()
				)));
			return details;
		}
		if (exception instanceof ConstraintViolationException constraintViolationException) {
			return constraintViolationException.getConstraintViolations().stream()
				.map(violation -> new ErrorResponse.ErrorDetail(
					violation.getPropertyPath().toString(),
					violation.getMessage()
				))
				.toList();
		}
		if (exception instanceof MethodArgumentTypeMismatchException methodArgumentTypeMismatchException) {
			return List.of(new ErrorResponse.ErrorDetail(
				methodArgumentTypeMismatchException.getName(),
				"요청 파라미터 형식이 올바르지 않습니다."
			));
		}
		if (exception instanceof MissingServletRequestParameterException missingServletRequestParameterException) {
			return List.of(new ErrorResponse.ErrorDetail(
				missingServletRequestParameterException.getParameterName(),
				"필수 요청 파라미터입니다."
			));
		}
		if (exception instanceof MissingPathVariableException missingPathVariableException) {
			return List.of(new ErrorResponse.ErrorDetail(
				missingPathVariableException.getVariableName(),
				"필수 경로 변수입니다."
			));
		}
		if (exception instanceof HttpMessageNotReadableException) {
			return List.of(new ErrorResponse.ErrorDetail(null, "요청 본문을 읽을 수 없습니다."));
		}
		return List.of(new ErrorResponse.ErrorDetail(null, exception.getMessage()));
	}

	private String correlationId(HttpServletRequest request) {
		String correlationId = request.getHeader(CORRELATION_ID_HEADER);
		if (correlationId == null || correlationId.isBlank()) {
			return null;
		}
		return correlationId;
	}

	private String detail(CardServiceException exception) {
		if (exception instanceof InvalidPaymentUsageEventRequestException invalidPaymentUsageEventRequestException) {
			return invalidPaymentUsageEventRequestException.getDetail();
		}
		return null;
	}
}
