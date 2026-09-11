package com.streamvault.ingestion.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.streamvault.ingestion.config.IngestionProperties;
import com.streamvault.ingestion.exception.InvalidVideoException;

class IngestionMaxSizeGuardTest {

	private IngestionMaxSizeGuard guard;

	@BeforeEach
	void setUp() {
		guard = new IngestionMaxSizeGuard(new IngestionProperties(1024, List.of("video/mp4")));
	}

	@Test
	void allowsSizeAtLimit() {
		assertDoesNotThrow(() -> guard.check(1024));
	}

	@Test
	void rejectsSizeOverLimit() {
		InvalidVideoException ex = assertThrows(InvalidVideoException.class, () -> guard.check(1025));
		assertEquals("file exceeds max size of 1024 bytes", ex.getMessage());
	}
}
