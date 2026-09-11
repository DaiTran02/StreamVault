package com.streamvault.ingestion.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.streamvault.ingestion.service.VideoService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/videos")
public class VideoController {

	private final VideoService videoService;

	public VideoController(VideoService videoService) {
		this.videoService = videoService;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Mono<ResponseEntity<VideoResponse>> upload(@RequestPart("file") FilePart file) {
		return videoService.upload(file)
				.map(VideoResponse::from)
				.map(body -> ResponseEntity.status(HttpStatus.CREATED).body(body));
	}

	@GetMapping("/{id}")
	public Mono<VideoResponse> get(@PathVariable UUID id) {
		return videoService.getById(id).map(VideoResponse::from);
	}

	@GetMapping("/{id}/detections")
	public Mono<VideoDetectionsResponse> detections(@PathVariable UUID id) {
		return videoService.getWithDetections(id)
				.map(result -> new VideoDetectionsResponse(
						VideoResponse.from(result.video()),
						result.detections().stream().map(DetectionResponse::from).toList()));
	}
}
