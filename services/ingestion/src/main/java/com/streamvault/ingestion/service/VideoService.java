package com.streamvault.ingestion.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;

import com.streamvault.ingestion.entity.Detection;
import com.streamvault.ingestion.entity.Video;
import com.streamvault.ingestion.entity.VideoStatus;
import com.streamvault.ingestion.exception.VideoNotFoundException;
import com.streamvault.ingestion.kafka.VideoUploadedEvent;
import com.streamvault.ingestion.kafka.VideoUploadedPublisher;
import com.streamvault.ingestion.repository.DetectionRepository;
import com.streamvault.ingestion.repository.VideoRepository;
import com.streamvault.ingestion.s3.S3VideoStorage;

import reactor.core.publisher.Mono;

@Service
public class VideoService {

	private final VideoUploadValidator validator;
	private final S3VideoStorage s3VideoStorage;
	private final VideoRepository videoRepository;
	private final DetectionRepository detectionRepository;
	private final VideoUploadedPublisher publisher;
	private final IngestionMaxSizeGuard maxSizeGuard;

	public VideoService(
			VideoUploadValidator validator,
			S3VideoStorage s3VideoStorage,
			VideoRepository videoRepository,
			DetectionRepository detectionRepository,
			VideoUploadedPublisher publisher,
			IngestionMaxSizeGuard maxSizeGuard) {
		this.validator = validator;
		this.s3VideoStorage = s3VideoStorage;
		this.videoRepository = videoRepository;
		this.detectionRepository = detectionRepository;
		this.publisher = publisher;
		this.maxSizeGuard = maxSizeGuard;
	}

	public Mono<Video> upload(FilePart file) {
		validator.validate(file);
		UUID id = UUID.randomUUID();
		String filename = sanitizeFilename(file.filename());
		String contentType = file.headers().getContentType().toString();
		String key = "videos/%s/%s".formatted(id, filename);
		Instant now = Instant.now();

		return s3VideoStorage.put(key, contentType, file)
				.flatMap(sizeBytes -> {
					maxSizeGuard.check(sizeBytes);
					Video video = Video.builder()
							.id(id)
							.originalFilename(file.filename())
							.contentType(contentType)
							.sizeBytes(sizeBytes)
							.s3Bucket(s3VideoStorage.bucket())
							.s3Key(key)
							.status(VideoStatus.STORED)
							.createdAt(now)
							.updatedAt(now)
							.build();
					return videoRepository.save(video);
				})
				.flatMap(saved -> publisher
						.publish(new VideoUploadedEvent(
								saved.getId(),
								saved.getS3Bucket(),
								saved.getS3Key(),
								saved.getContentType()))
						.thenReturn(saved));
	}

	public Mono<Video> getById(UUID id) {
		return videoRepository.findById(id)
				.switchIfEmpty(Mono.error(new VideoNotFoundException(id)));
	}

	public Mono<VideoWithDetections> getWithDetections(UUID id) {
		return getById(id)
				.flatMap(video -> detectionRepository.findByVideoId(id)
						.collectList()
						.map(detections -> new VideoWithDetections(video, detections)));
	}

	static String sanitizeFilename(String filename) {
		String name = filename.replace('\\', '/');
		int slash = name.lastIndexOf('/');
		if (slash >= 0) {
			name = name.substring(slash + 1);
		}
		name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
		if (name.isBlank() || name.replaceAll("[._-]", "").isBlank()) {
			return "video.bin";
		}
		return name;
	}

	public record VideoWithDetections(Video video, List<Detection> detections) {
	}
}
