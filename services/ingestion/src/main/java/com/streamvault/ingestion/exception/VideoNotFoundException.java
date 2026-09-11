package com.streamvault.ingestion.exception;

import java.util.UUID;

public class VideoNotFoundException extends RuntimeException {

	public VideoNotFoundException(UUID id) {
		super("Video not found: " + id);
	}
}
