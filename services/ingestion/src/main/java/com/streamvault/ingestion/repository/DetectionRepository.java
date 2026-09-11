package com.streamvault.ingestion.repository;

import java.util.UUID;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.streamvault.ingestion.entity.Detection;

import reactor.core.publisher.Flux;

public interface DetectionRepository extends ReactiveCrudRepository<Detection, UUID> {

	Flux<Detection> findByVideoId(UUID videoId);
}
