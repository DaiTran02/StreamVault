package com.streamvault.ingestion.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebInputException;

import com.streamvault.ingestion.exception.InvalidVideoException;
import com.streamvault.ingestion.exception.VideoNotFoundException;

class ApiExceptionHandlerTest {

	private final ApiExceptionHandler handler = new ApiExceptionHandler();

	@Test
	void invalidVideoIsBadRequest() {
		ResponseEntity<?> response = handler.invalid(new InvalidVideoException("unsupported content type: text/plain"));
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertEquals("unsupported content type: text/plain", ((Map<?, ?>) response.getBody()).get("error"));
	}

	@Test
	void oversizedVideoIsPayloadTooLarge() {
		ResponseEntity<?> response = handler.invalid(new InvalidVideoException("file exceeds max size of 1024 bytes"));
		assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
	}

	@Test
	void missingVideoIsNotFound() {
		UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
		ResponseEntity<?> response = handler.notFound(new VideoNotFoundException(id));
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEquals("Video not found: " + id, ((Map<?, ?>) response.getBody()).get("error"));
	}

	@Test
	void badInputIsBadRequest() {
		ResponseEntity<?> response = handler.badInput(new ServerWebInputException("invalid request"));
		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
	}
}
