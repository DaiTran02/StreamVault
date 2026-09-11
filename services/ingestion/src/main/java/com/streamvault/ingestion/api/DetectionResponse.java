package com.streamvault.ingestion.api;

import java.time.Instant;
import java.util.UUID;

import com.streamvault.ingestion.entity.Detection;

public record DetectionResponse(
		UUID id,
		UUID videoId,
		String label,
		double confidence,
		double bboxX,
		double bboxY,
		double bboxW,
		double bboxH,
		int frameIndex,
		long tsMs,
		Instant createdAt) {

	public static DetectionResponse from(Detection detection) {
		return new DetectionResponse(
				detection.getId(),
				detection.getVideoId(),
				detection.getLabel(),
				detection.getConfidence(),
				detection.getBboxX(),
				detection.getBboxY(),
				detection.getBboxW(),
				detection.getBboxH(),
				detection.getFrameIndex(),
				detection.getTsMs(),
				detection.getCreatedAt());
	}
}
