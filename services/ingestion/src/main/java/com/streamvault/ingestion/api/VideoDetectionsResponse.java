package com.streamvault.ingestion.api;

import java.util.List;

public record VideoDetectionsResponse(VideoResponse video, List<DetectionResponse> detections) {
}
