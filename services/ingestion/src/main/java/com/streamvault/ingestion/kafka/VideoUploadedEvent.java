package com.streamvault.ingestion.kafka;

import java.util.UUID;

public record VideoUploadedEvent(UUID videoId, String bucket, String key, String contentType) {
}
