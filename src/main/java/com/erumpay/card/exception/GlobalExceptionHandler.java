package com.erumpay.card.exception;

import com.erumpay.card.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(CardServiceException.class)
	public ResponseEntity<ErrorResponse> handleCardServiceException(CardServiceException exception) {
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

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(new ErrorResponse("INVALID_REQUEST", message));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException() {
		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(new ErrorResponse("INVALID_REQUEST", "요청 형식 오류"));
	}

	@ExceptionHandler(Throwable.class)
	public ResponseEntity<ErrorResponse> handleThrowable() {
		return ResponseEntity
			.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(new ErrorResponse("INTERNAL_ERROR", "서버 오류가 발생했습니다."));
	}
}
