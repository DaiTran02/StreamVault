package com.streamvault.auth_service.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.streamvault.auth_service.dto.ApiResponse;
import com.streamvault.auth_service.exception.AuthException;

@RestControllerAdvice
public class AuthExceptionHandler {

	private final String serviceName;

	public AuthExceptionHandler(@Value("${spring.application.name:auth-service}") String serviceName) {
		this.serviceName = serviceName;
	}

	@ExceptionHandler(AuthException.class)
	public ResponseEntity<ApiResponse<Void>> auth(AuthException ex) {
		return ApiResponse.errorEntity(ex.getStatus(), serviceName, ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> invalid(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getField() + " " + error.getDefaultMessage())
				.orElse("invalid request");
		return ApiResponse.errorEntity(HttpStatus.BAD_REQUEST, serviceName, message);
	}
}
