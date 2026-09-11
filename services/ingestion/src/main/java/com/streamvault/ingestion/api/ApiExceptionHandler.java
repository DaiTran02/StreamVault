package com.streamvault.ingestion.api;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;

import com.streamvault.ingestion.exception.InvalidVideoException;
import com.streamvault.ingestion.exception.VideoNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(InvalidVideoException.class)
	public ResponseEntity<Map<String, String>> invalid(InvalidVideoException ex) {
		HttpStatus status = ex.getMessage() != null && ex.getMessage().contains("exceeds max size")
				? HttpStatus.PAYLOAD_TOO_LARGE
				: HttpStatus.BAD_REQUEST;
		return ResponseEntity.status(status).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(VideoNotFoundException.class)
	public ResponseEntity<Map<String, String>> notFound(VideoNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(ServerWebInputException.class)
	public ResponseEntity<Map<String, String>> badInput(ServerWebInputException ex) {
		String message = ex.getMessage() != null ? ex.getMessage() : "invalid request";
		return ResponseEntity.badRequest().body(Map.of("error", message));
	}
}
