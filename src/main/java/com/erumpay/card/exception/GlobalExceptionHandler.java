package com.erumpay.card.exception;

import com.erumpay.card.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(CardServiceException.class)
	public ResponseEntity<ErrorResponse> handleCardServiceException(CardServiceException exception) {
		log.warn(
			"Card service exception handled. status={}, code={}, message={}",
			exception.getStatus(),
			exception.getCode(),
			exception.getMessage()
		);
		return ResponseEntity
			.status(exception.getStatus())
			.body(new ErrorResponse(exception.getCode(), exception.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
		MethodArgumentNotValidException exception
	) {
		String message = exception.getBindingResult().getFieldErrors().stream()
			.findFirst()
			.map(fieldError -> fieldError.getDefaultMessage())
			.orElse("요청 형식 오류");

		log.warn("Request validation failed. message={}", message);

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(new ErrorResponse("INVALID_REQUEST", message));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
		HttpMessageNotReadableException exception
	) {
		log.warn("Request body parse failed. exceptionType={}", exception.getClass().getSimpleName());

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(new ErrorResponse("INVALID_REQUEST", "요청 형식 오류"));
	}

	@ExceptionHandler({
		MissingServletRequestParameterException.class,
		MethodArgumentTypeMismatchException.class
	})
	public ResponseEntity<ErrorResponse> handleInvalidRequestParameter(Exception exception) {
		log.warn("Invalid request parameter. exceptionType={}", exception.getClass().getSimpleName());

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(new ErrorResponse("INVALID_REQUEST", "요청 파라미터 오류"));
	}

	@ExceptionHandler(Throwable.class)
	public ResponseEntity<ErrorResponse> handleThrowable(Throwable exception) {
		log.error("Unhandled exception occurred in GlobalExceptionHandler.", exception);

		return ResponseEntity
			.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(new ErrorResponse("INTERNAL_ERROR", "서버 오류가 발생했습니다."));
	}
}
