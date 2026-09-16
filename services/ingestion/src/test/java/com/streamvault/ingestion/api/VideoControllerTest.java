package com.streamvault.ingestion.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.streamvault.ingestion.dto.ApiResponse;
import com.streamvault.ingestion.dto.VideoResponse;
import com.streamvault.ingestion.entity.CustodyEvent;
import com.streamvault.ingestion.entity.Detection;
import com.streamvault.ingestion.entity.Video;
import com.streamvault.ingestion.entity.VideoStatus;
import com.streamvault.ingestion.service.VideoService;
import com.streamvault.ingestion.service.VideoService.VideoWithDetections;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class VideoControllerTest {

	private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Mock
	private VideoService videoService;

	@Mock
	private FilePart file;

	private VideoController controller;

	@BeforeEach
	void setUp() {
		controller = new VideoController(videoService, "ingestion");
	}

	@Test
	void uploadReturnsCreated() {
		Video video = video();
		when(videoService.upload(file, USER_ID)).thenReturn(Mono.just(video));

		StepVerifier.create(controller.upload(file, auth()))
				.assertNext(response -> {
					assertEquals(HttpStatus.CREATED, response.getStatusCode());
					ApiResponse<VideoResponse> envelope = response.getBody();
					assertEquals(201, envelope.status());
					assertEquals("ingestion", envelope.service());
					assertEquals("success", envelope.message());
					assertEquals(video.getId(), envelope.data().id());
					assertEquals(VideoStatus.STORED, envelope.data().status());
					assertEquals(USER_ID, envelope.data().userId());
				})
				.verifyComplete();
	}

	@Test
	void getReturnsVideo() {
		Video video = video();
		when(videoService.getById(video.getId(), USER_ID)).thenReturn(Mono.just(video));

		StepVerifier.create(controller.get(video.getId(), auth()))
				.assertNext(response -> {
					assertEquals(video.getId(), response.getBody().data().id());
					assertEquals("clip.mp4", response.getBody().data().originalFilename());
					assertEquals("abc123", response.getBody().data().contentSha256());
					assertEquals(200, response.getBody().status());
					assertEquals("success", response.getBody().message());
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
		when(videoService.getWithDetections(video.getId(), USER_ID))
				.thenReturn(Mono.just(new VideoWithDetections(video, List.of(detection))));

		StepVerifier.create(controller.detections(video.getId(), auth()))
				.assertNext(response -> {
					assertEquals(video.getId(), response.getBody().data().video().id());
					assertEquals(1, response.getBody().data().detections().size());
					assertEquals("face", response.getBody().data().detections().get(0).label());
				})
				.verifyComplete();
	}

	@Test
	void custodyReturnsEvents() {
		Video video = video();
		CustodyEvent event = CustodyEvent.builder()
				.id(UUID.randomUUID())
				.videoId(video.getId())
				.userId(USER_ID)
				.action("INGEST")
				.contentSha256("abc123")
				.previousChainHash("0".repeat(64))
				.chainHash("def")
				.createdAt(video.getCreatedAt())
				.build();
		when(videoService.getCustody(video.getId(), USER_ID)).thenReturn(Mono.just(List.of(event)));

		StepVerifier.create(controller.custody(video.getId(), auth()))
				.assertNext(response -> {
					assertEquals(1, response.getBody().data().size());
					assertEquals("INGEST", response.getBody().data().get(0).action());
				})
				.verifyComplete();
	}

	private static Authentication auth() {
		return new TestingAuthenticationToken(USER_ID.toString(), "n/a");
	}

	private static Video video() {
		UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
		Instant now = Instant.parse("2026-01-01T00:00:00Z");
		return Video.builder()
				.id(id)
				.userId(USER_ID)
				.originalFilename("clip.mp4")
				.contentType("video/mp4")
				.sizeBytes(512)
				.contentSha256("abc123")
				.s3Bucket("test-bucket")
				.s3Key("videos/%s/clip.mp4".formatted(id))
				.status(VideoStatus.STORED)
				.createdAt(now)
				.updatedAt(now)
				.build();
	}
}
