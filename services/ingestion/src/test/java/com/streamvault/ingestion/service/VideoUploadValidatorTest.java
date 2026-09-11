package com.streamvault.ingestion.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;

import com.streamvault.ingestion.config.IngestionProperties;
import com.streamvault.ingestion.exception.InvalidVideoException;

class VideoUploadValidatorTest {

	private VideoUploadValidator validator;

	@BeforeEach
	void setUp() {
		validator = new VideoUploadValidator(new IngestionProperties(
				1024,
				List.of("video/mp4", "video/quicktime", "video/webm")));
	}

	@Test
	void acceptsMp4() {
		assertDoesNotThrow(() -> validator.validate(file("clip.mp4", MediaType.valueOf("video/mp4"), 512)));
	}

	@Test
	void rejectsUnsupportedType() {
		InvalidVideoException ex = assertThrows(InvalidVideoException.class,
				() -> validator.validate(file("notes.txt", MediaType.TEXT_PLAIN, 12)));
		assertEquals("unsupported content type: text/plain", ex.getMessage());
	}

	@Test
	void rejectsMissingFilename() {
		assertThrows(InvalidVideoException.class,
				() -> validator.validate(file("  ", MediaType.valueOf("video/mp4"), 12)));
	}

	@Test
	void rejectsDeclaredLengthOverMax() {
		InvalidVideoException ex = assertThrows(InvalidVideoException.class,
				() -> validator.validate(file("clip.mp4", MediaType.valueOf("video/mp4"), 2048)));
		assertEquals("file exceeds max size of 1024 bytes", ex.getMessage());
	}

	@Test
	void rejectsNullFile() {
		InvalidVideoException ex = assertThrows(InvalidVideoException.class, () -> validator.validate(null));
		assertEquals("file part is required", ex.getMessage());
	}

	@Test
	void rejectsNullFilename() {
		assertThrows(InvalidVideoException.class,
				() -> validator.validate(file(null, MediaType.valueOf("video/mp4"), 12)));
	}

	@Test
	void acceptsTypeIgnoringCase() {
		assertDoesNotThrow(() -> validator.validate(file("clip.mp4", MediaType.valueOf("VIDEO/MP4"), 512)));
	}

	@Test
	void rejectsMissingContentType() {
		FilePart file = mock(FilePart.class);
		when(file.filename()).thenReturn("clip.mp4");
		when(file.headers()).thenReturn(new HttpHeaders());
		InvalidVideoException ex = assertThrows(InvalidVideoException.class, () -> validator.validate(file));
		assertEquals("unsupported content type: ", ex.getMessage());
	}

	private static FilePart file(String name, MediaType type, long length) {
		FilePart file = mock(FilePart.class);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(type);
		headers.setContentLength(length);
		when(file.filename()).thenReturn(name);
		when(file.headers()).thenReturn(headers);
		return file;
	}
}
