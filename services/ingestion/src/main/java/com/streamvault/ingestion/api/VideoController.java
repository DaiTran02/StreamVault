package com.streamvault.ingestion.api;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.streamvault.ingestion.dto.ApiResponse;
import com.streamvault.ingestion.dto.CustodyEventResponse;
import com.streamvault.ingestion.dto.DetectionResponse;
import com.streamvault.ingestion.dto.VideoDetectionsResponse;
import com.streamvault.ingestion.dto.VideoResponse;
import com.streamvault.ingestion.security.UserIds;
import com.streamvault.ingestion.service.VideoService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/videos")
public class VideoController {

	private final VideoService videoService;
	private final String serviceName;

	public VideoController(VideoService videoService, @Value("${spring.application.name:ingestion}") String serviceName) {
		this.videoService = videoService;
		this.serviceName = serviceName;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Mono<ResponseEntity<ApiResponse<VideoResponse>>> upload(
			@RequestPart("file") FilePart file,
			Authentication authentication) {
		UUID userId = UserIds.from(authentication);
		return videoService.upload(file, userId)
				.map(VideoResponse::from)
				.map(body -> ApiResponse.created(serviceName, body));
	}

	@GetMapping("/{id}")
	public Mono<ResponseEntity<ApiResponse<VideoResponse>>> get(@PathVariable UUID id, Authentication authentication) {
		return videoService.getById(id, UserIds.from(authentication))
				.map(VideoResponse::from)
				.map(body -> ApiResponse.ok(serviceName, body));
	}

	@GetMapping("/{id}/detections")
	public Mono<ResponseEntity<ApiResponse<VideoDetectionsResponse>>> detections(
			@PathVariable UUID id,
			Authentication authentication) {
		return videoService.getWithDetections(id, UserIds.from(authentication))
				.map(result -> new VideoDetectionsResponse(
						VideoResponse.from(result.video()),
						result.detections().stream().map(DetectionResponse::from).toList()))
				.map(body -> ApiResponse.ok(serviceName, body));
	}

	@GetMapping("/{id}/custody")
	public Mono<ResponseEntity<ApiResponse<List<CustodyEventResponse>>>> custody(
			@PathVariable UUID id,
			Authentication authentication) {
		return videoService.getCustody(id, UserIds.from(authentication))
				.map(events -> events.stream().map(CustodyEventResponse::from).toList())
				.map(body -> ApiResponse.ok(serviceName, body));
	}
}
