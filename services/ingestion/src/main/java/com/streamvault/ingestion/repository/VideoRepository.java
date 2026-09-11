package com.streamvault.ingestion.repository;

import java.util.UUID;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.streamvault.ingestion.entity.Video;

public interface VideoRepository extends ReactiveCrudRepository<Video, UUID> {
}
