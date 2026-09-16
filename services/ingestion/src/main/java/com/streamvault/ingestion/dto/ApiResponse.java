package com.streamvault.ingestion.dto;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public record ApiResponse<T>(int status, String service, String message, T data) {

	public static <T> ApiResponse<T> success(int status, String service, T data) {
		return new ApiResponse<>(status, service, "success", data);
	}

	public static <T> ApiResponse<T> error(int status, String service, String message) {
		return new ApiResponse<>(status, service, message, null);
	}

	public static <T> ResponseEntity<ApiResponse<T>> ok(String service, T data) {
		return entity(HttpStatus.OK, service, data);
	}

	public static <T> ResponseEntity<ApiResponse<T>> created(String service, T data) {
		return entity(HttpStatus.CREATED, service, data);
	}

	public static <T> ResponseEntity<ApiResponse<T>> entity(HttpStatus status, String service, T data) {
		return ResponseEntity.status(status).body(success(status.value(), service, data));
	}

	public static <T> ResponseEntity<ApiResponse<T>> errorEntity(HttpStatus status, String service, String message) {
		return ResponseEntity.status(status).body(error(status.value(), service, message));
	}
}
