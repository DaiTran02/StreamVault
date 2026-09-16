package com.streamvault.ingestion.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebInputException;

import com.streamvault.ingestion.dto.ApiResponse;
import com.streamvault.ingestion.exception.CustodyIntegrityException;
import com.streamvault.ingestion.exception.InvalidVideoException;
import com.streamvault.ingestion.exception.VideoAccessDeniedException;
import com.streamvault.ingestion.exception.VideoNotFoundException;

class ApiExceptionHandlerTest {

	private final ApiExceptionHandler handler = new ApiExceptionHandler("ingestion");

	@Test
	void invalidVideoIsBadRequest() {
		ResponseEntity<ApiResponse<Void>> response = handler.invalid(
				new InvalidVideoException("unsupported content type: text/plain"));
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEnvelope(response.getBody(), 400, "unsupported content type: text/plain");
	}

	@Test
	void oversizedVideoIsPayloadTooLarge() {
		ResponseEntity<ApiResponse<Void>> response = handler.invalid(
				new InvalidVideoException("file exceeds max size of 1024 bytes"));
		assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
		assertEnvelope(response.getBody(), 413, "file exceeds max size of 1024 bytes");
	}

	@Test
	void missingVideoIsNotFound() {
		UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
		ResponseEntity<ApiResponse<Void>> response = handler.notFound(new VideoNotFoundException(id));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEnvelope(response.getBody(), 404, "Video not found: " + id);
	}

	@Test
	void accessDeniedIsForbidden() {
		ResponseEntity<ApiResponse<Void>> response = handler.forbidden(new VideoAccessDeniedException());
		assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
		assertEquals(403, response.getBody().status());
		assertEquals("ingestion", response.getBody().service());
		assertNull(response.getBody().data());
	}

	@Test
	void brokenCustodyIsConflict() {
		UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
		ResponseEntity<ApiResponse<Void>> response = handler.custody(new CustodyIntegrityException(id));
		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
		assertEquals(409, response.getBody().status());
	}

	@Test
	void badInputIsBadRequest() {
		ResponseEntity<ApiResponse<Void>> response = handler.badInput(new ServerWebInputException("invalid request"));
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEnvelope(response.getBody(), 400, "invalid request");
	}

	private static void assertEnvelope(ApiResponse<Void> body, int status, String message) {
		assertEquals(status, body.status());
		assertEquals("ingestion", body.service());
		assertEquals(message, body.message());
		assertNull(body.data());
	}
}
