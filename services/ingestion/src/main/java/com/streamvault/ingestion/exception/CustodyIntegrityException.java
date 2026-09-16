package com.streamvault.ingestion.exception;

import java.util.UUID;

public class CustodyIntegrityException extends RuntimeException {

	public CustodyIntegrityException(UUID videoId) {
		super("Chain of custody is broken for video: " + videoId);
	}

	public CustodyIntegrityException(String message) {
		super(message);
	}
}
