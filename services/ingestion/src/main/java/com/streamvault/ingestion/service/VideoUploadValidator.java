package com.streamvault.ingestion.service;

import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;

import com.streamvault.ingestion.config.IngestionProperties;
import com.streamvault.ingestion.exception.InvalidVideoException;

@Component
public class VideoUploadValidator {

	private final IngestionProperties properties;

	public VideoUploadValidator(IngestionProperties properties) {
		this.properties = properties;
	}

	public void validate(FilePart file) {
		if (file == null) {
			throw new InvalidVideoException("file part is required");
		}
		String filename = file.filename();
		if (filename == null || filename.isBlank()) {
			throw new InvalidVideoException("filename is required");
		}
		MediaType contentType = file.headers().getContentType();
		String type = contentType != null ? contentType.toString() : "";
		boolean allowed = properties.allowedContentTypes().stream()
				.anyMatch(allowedType -> allowedType.equalsIgnoreCase(type));
		if (!allowed) {
			throw new InvalidVideoException("unsupported content type: " + type);
		}
		long declaredLength = file.headers().getContentLength();
		if (declaredLength > properties.maxFileSizeBytes()) {
			throw new InvalidVideoException("file exceeds max size of " + properties.maxFileSizeBytes() + " bytes");
		}
	}
}
