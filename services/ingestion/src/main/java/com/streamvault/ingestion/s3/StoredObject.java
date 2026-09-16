package com.streamvault.ingestion.s3;

public record StoredObject(long sizeBytes, String sha256Hex) {
}
