package com.streamvault.ingestion.dto;

import java.time.Instant;
import java.util.UUID;

import com.streamvault.ingestion.entity.Video;
import com.streamvault.ingestion.entity.VideoStatus;

public record VideoResponse(
		UUID id,
		UUID userId,
		String originalFilename,
		String contentType,
		long sizeBytes,
		String contentSha256,
		String s3Bucket,
		String s3Key,
		VideoStatus status,
		String errorMessage,
		Instant createdAt,
		Instant updatedAt) {

	public static VideoResponse from(Video video) {
		return new VideoResponse(
				video.getId(),
				video.getUserId(),
				video.getOriginalFilename(),
				video.getContentType(),
				video.getSizeBytes(),
				video.getContentSha256(),
				video.getS3Bucket(),
				video.getS3Key(),
				video.getStatus(),
				video.getErrorMessage(),
				video.getCreatedAt(),
				video.getUpdatedAt());
	}
}
