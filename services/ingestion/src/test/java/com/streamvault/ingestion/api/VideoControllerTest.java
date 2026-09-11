package com.streamvault.ingestion.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;

import com.streamvault.ingestion.entity.Detection;
import com.streamvault.ingestion.entity.Video;
import com.streamvault.ingestion.entity.VideoStatus;
import com.streamvault.ingestion.service.VideoService;
import com.streamvault.ingestion.service.VideoService.VideoWithDetections;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class VideoControllerTest {

	@Mock
	private VideoService videoService;

	@Mock
	private FilePart file;

	@InjectMocks
	private VideoController controller;

	@Test
	void uploadReturnsCreated() {
		Video video = video();
		when(videoService.upload(file)).thenReturn(Mono.just(video));

		StepVerifier.create(controller.upload(file))
				.assertNext(response -> {
					assertEquals(HttpStatus.CREATED, response.getStatusCode());
					assertEquals(video.getId(), response.getBody().id());
					assertEquals(VideoStatus.STORED, response.getBody().status());
				})
				.verifyComplete();
	}

	@Test
	void getReturnsVideo() {
		Video video = video();
		when(videoService.getById(video.getId())).thenReturn(Mono.just(video));

		StepVerifier.create(controller.get(video.getId()))
				.assertNext(body -> {
					assertEquals(video.getId(), body.id());
					assertEquals("clip.mp4", body.originalFilename());
				})
				.verifyComplete();
	}

	@Test
	void detectionsReturnsVideoAndDetections() {
		Video video = video();
		Detection detection = Detection.builder()
				.id(UUID.randomUUID())
				.videoId(video.getId())
				.label("face")
				.confidence(0.8)
				.bboxX(1)
				.bboxY(2)
				.bboxW(3)
				.bboxH(4)
				.frameIndex(0)
				.tsMs(0)
				.createdAt(video.getCreatedAt())
				.build();
		when(videoService.getWithDetections(video.getId()))
				.thenReturn(Mono.just(new VideoWithDetections(video, List.of(detection))));

		StepVerifier.create(controller.detections(video.getId()))
				.assertNext(body -> {
					assertEquals(video.getId(), body.video().id());
					assertEquals(1, body.detections().size());
					assertEquals("face", body.detections().get(0).label());
				})
				.verifyComplete();
	}

	private static Video video() {
		UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
		Instant now = Instant.parse("2026-01-01T00:00:00Z");
		return Video.builder()
				.id(id)
				.originalFilename("clip.mp4")
				.contentType("video/mp4")
				.sizeBytes(512)
				.s3Bucket("test-bucket")
				.s3Key("videos/%s/clip.mp4".formatted(id))
				.status(VideoStatus.STORED)
				.createdAt(now)
				.updatedAt(now)
				.build();
	}
}
