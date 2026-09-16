package com.streamvault.ingestion.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;

import com.streamvault.ingestion.dto.ApiResponse;
import com.streamvault.ingestion.exception.CustodyIntegrityException;
import com.streamvault.ingestion.exception.InvalidVideoException;
import com.streamvault.ingestion.exception.VideoAccessDeniedException;
import com.streamvault.ingestion.exception.VideoNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

	private final String serviceName;

	public ApiExceptionHandler(@Value("${spring.application.name:ingestion}") String serviceName) {
		this.serviceName = serviceName;
	}

	@ExceptionHandler(InvalidVideoException.class)
	public ResponseEntity<ApiResponse<Void>> invalid(InvalidVideoException ex) {
		HttpStatus status = ex.getMessage() != null && ex.getMessage().contains("exceeds max size")
				? HttpStatus.PAYLOAD_TOO_LARGE
				: HttpStatus.BAD_REQUEST;
		return ApiResponse.errorEntity(status, serviceName, ex.getMessage());
	}

	@ExceptionHandler(VideoNotFoundException.class)
	public ResponseEntity<ApiResponse<Void>> notFound(VideoNotFoundException ex) {
		return ApiResponse.errorEntity(HttpStatus.NOT_FOUND, serviceName, ex.getMessage());
	}

	@ExceptionHandler(VideoAccessDeniedException.class)
	public ResponseEntity<ApiResponse<Void>> forbidden(VideoAccessDeniedException ex) {
		return ApiResponse.errorEntity(HttpStatus.FORBIDDEN, serviceName, ex.getMessage());
	}

	@ExceptionHandler(CustodyIntegrityException.class)
	public ResponseEntity<ApiResponse<Void>> custody(CustodyIntegrityException ex) {
		return ApiResponse.errorEntity(HttpStatus.CONFLICT, serviceName, ex.getMessage());
	}

	@ExceptionHandler(ServerWebInputException.class)
	public ResponseEntity<ApiResponse<Void>> badInput(ServerWebInputException ex) {
		String message = ex.getReason() != null ? ex.getReason() : "invalid request";
		return ApiResponse.errorEntity(HttpStatus.BAD_REQUEST, serviceName, message);
	}
}
