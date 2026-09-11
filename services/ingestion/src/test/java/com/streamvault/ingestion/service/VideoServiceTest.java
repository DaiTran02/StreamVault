package com.streamvault.ingestion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;

import com.streamvault.ingestion.entity.Detection;
import com.streamvault.ingestion.entity.Video;
import com.streamvault.ingestion.entity.VideoStatus;
import com.streamvault.ingestion.exception.InvalidVideoException;
import com.streamvault.ingestion.exception.VideoNotFoundException;
import com.streamvault.ingestion.kafka.VideoUploadedEvent;
import com.streamvault.ingestion.kafka.VideoUploadedPublisher;
import com.streamvault.ingestion.repository.DetectionRepository;
import com.streamvault.ingestion.repository.VideoRepository;
import com.streamvault.ingestion.s3.S3VideoStorage;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class VideoServiceTest {

	@Mock
	private VideoUploadValidator validator;

	@Mock
	private S3VideoStorage s3VideoStorage;

	@Mock
	private VideoRepository videoRepository;

	@Mock
	private DetectionRepository detectionRepository;

	@Mock
	private VideoUploadedPublisher publisher;

	@Mock
	private IngestionMaxSizeGuard maxSizeGuard;

	@InjectMocks
	private VideoService videoService;

	@Test
	void uploadStoresMetadataAndPublishesEvent() {
		FilePart file = file("../../my clip.mp4", MediaType.valueOf("video/mp4"));
		when(s3VideoStorage.bucket()).thenReturn("test-bucket");
		when(s3VideoStorage.put(anyString(), eq("video/mp4"), any())).thenReturn(Mono.just(512L));
		when(videoRepository.save(any(Video.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
		when(publisher.publish(any())).thenReturn(Mono.empty());

		StepVerifier.create(videoService.upload(file))
				.assertNext(saved -> {
					assertEquals("../../my clip.mp4", saved.getOriginalFilename());
					assertEquals("video/mp4", saved.getContentType());
					assertEquals(512L, saved.getSizeBytes());
					assertEquals("test-bucket", saved.getS3Bucket());
					assertEquals(VideoStatus.STORED, saved.getStatus());
					assertEquals("videos/%s/my_clip.mp4".formatted(saved.getId()), saved.getS3Key());
				})
				.verifyComplete();

		ArgumentCaptor<VideoUploadedEvent> event = ArgumentCaptor.forClass(VideoUploadedEvent.class);
		verify(publisher).publish(event.capture());
		VideoUploadedEvent published = event.getValue();
		assertEquals("test-bucket", published.bucket());
		assertEquals("video/mp4", published.contentType());
		assertEquals("videos/%s/my_clip.mp4".formatted(published.videoId()), published.key());
		verify(validator).validate(file);
		verify(maxSizeGuard).check(512L);
	}

	@Test
	void uploadPropagatesValidationFailureSynchronously() {
		FilePart file = file("clip.mp4", MediaType.valueOf("video/mp4"));
		doThrow(new InvalidVideoException("file part is required")).when(validator).validate(file);

		InvalidVideoException ex = assertThrows(InvalidVideoException.class, () -> videoService.upload(file));
		assertEquals("file part is required", ex.getMessage());
		verify(s3VideoStorage, never()).put(anyString(), anyString(), any());
	}

	@Test
	void uploadDoesNotPersistWhenUploadedSizeExceedsMax() {
		FilePart file = file("clip.mp4", MediaType.valueOf("video/mp4"));
		when(s3VideoStorage.put(anyString(), eq("video/mp4"), any())).thenReturn(Mono.just(2048L));
		doThrow(new InvalidVideoException("file exceeds max size of 1024 bytes")).when(maxSizeGuard).check(2048L);

		StepVerifier.create(videoService.upload(file))
				.expectErrorSatisfies(error -> {
					assertInstanceOf(InvalidVideoException.class, error);
					assertEquals("file exceeds max size of 1024 bytes", error.getMessage());
				})
				.verify();

		verify(videoRepository, never()).save(any());
		verify(publisher, never()).publish(any());
	}

	@Test
	void getByIdReturnsVideo() {
		UUID id = UUID.randomUUID();
		Video video = video(id);
		when(videoRepository.findById(id)).thenReturn(Mono.just(video));

		StepVerifier.create(videoService.getById(id))
				.expectNext(video)
				.verifyComplete();
	}

	@Test
	void getByIdFailsWhenMissing() {
		UUID id = UUID.randomUUID();
		when(videoRepository.findById(id)).thenReturn(Mono.empty());

		StepVerifier.create(videoService.getById(id))
				.expectErrorSatisfies(error -> {
					assertInstanceOf(VideoNotFoundException.class, error);
					assertEquals("Video not found: " + id, error.getMessage());
				})
				.verify();
	}

	@Test
	void getWithDetectionsReturnsVideoAndDetections() {
		UUID id = UUID.randomUUID();
		Video video = video(id);
		Detection detection = Detection.builder()
				.id(UUID.randomUUID())
				.videoId(id)
				.label("person")
				.confidence(0.9)
				.build();
		when(videoRepository.findById(id)).thenReturn(Mono.just(video));
		when(detectionRepository.findByVideoId(id)).thenReturn(Flux.just(detection));

		StepVerifier.create(videoService.getWithDetections(id))
				.assertNext(result -> {
					assertEquals(video, result.video());
					assertEquals(1, result.detections().size());
					assertEquals("person", result.detections().get(0).getLabel());
				})
				.verifyComplete();
	}

	@Test
	void getWithDetectionsFailsWhenVideoMissing() {
		UUID id = UUID.randomUUID();
		when(videoRepository.findById(id)).thenReturn(Mono.empty());

		StepVerifier.create(videoService.getWithDetections(id))
				.expectError(VideoNotFoundException.class)
				.verify();

		verify(detectionRepository, never()).findByVideoId(any());
	}

	@Test
	void sanitizeFilenameStripsPathsAndUnsafeCharacters() {
		assertEquals("my_clip.mp4", VideoService.sanitizeFilename("../../my clip.mp4"));
		assertEquals("bar.mp4", VideoService.sanitizeFilename("C:\\foo\\bar.mp4"));
		assertEquals("clip.mp4", VideoService.sanitizeFilename("clip.mp4"));
		assertEquals("video.bin", VideoService.sanitizeFilename("***"));
		assertEquals("video.bin", VideoService.sanitizeFilename("..."));
		assertEquals("video.bin", VideoService.sanitizeFilename("___"));
	}

	private static FilePart file(String name, MediaType type) {
		FilePart file = org.mockito.Mockito.mock(FilePart.class);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(type);
		lenient().when(file.filename()).thenReturn(name);
		lenient().when(file.headers()).thenReturn(headers);
		return file;
	}

	private static Video video(UUID id) {
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
