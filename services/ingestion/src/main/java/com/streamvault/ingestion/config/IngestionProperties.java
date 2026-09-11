package com.streamvault.ingestion.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ingestion")
public record IngestionProperties(long maxFileSizeBytes, List<String> allowedContentTypes) {
}
