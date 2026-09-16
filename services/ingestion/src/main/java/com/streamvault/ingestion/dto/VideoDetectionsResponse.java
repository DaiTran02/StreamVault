package com.streamvault.ingestion.dto;

import java.util.List;

public record VideoDetectionsResponse(VideoResponse video, List<DetectionResponse> detections) {
}
