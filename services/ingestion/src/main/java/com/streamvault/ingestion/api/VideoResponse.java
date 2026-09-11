package com.streamvault.ingestion.api;

import java.time.Instant;
import java.util.UUID;

import com.streamvault.ingestion.entity.Video;
import com.streamvault.ingestion.entity.VideoStatus;

public record VideoResponse(
		UUID id,
		String originalFilename,
		String contentType,
		long sizeBytes,
		String s3Bucket,
		String s3Key,
		VideoStatus status,
		String errorMessage,
		Instant createdAt,
		Instant updatedAt) {

	public static VideoResponse from(Video video) {
		return new VideoResponse(
				video.getId(),
				video.getOriginalFilename(),
				video.getContentType(),
				video.getSizeBytes(),
				video.getS3Bucket(),
				video.getS3Key(),
				video.getStatus(),
				video.getErrorMessage(),
				video.getCreatedAt(),
				video.getUpdatedAt());
	}
}
