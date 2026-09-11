package com.streamvault.ingestion.service;

import org.springframework.stereotype.Component;

import com.streamvault.ingestion.config.IngestionProperties;
import com.streamvault.ingestion.exception.InvalidVideoException;

@Component
public class IngestionMaxSizeGuard {

	private final IngestionProperties properties;

	public IngestionMaxSizeGuard(IngestionProperties properties) {
		this.properties = properties;
	}

	public void check(long sizeBytes) {
		if (sizeBytes > properties.maxFileSizeBytes()) {
			throw new InvalidVideoException("file exceeds max size of " + properties.maxFileSizeBytes() + " bytes");
		}
	}
}
