package com.streamvault.ingestion.repository;

import java.util.UUID;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.streamvault.ingestion.entity.CustodyEvent;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CustodyEventRepository extends ReactiveCrudRepository<CustodyEvent, UUID> {

	Flux<CustodyEvent> findByVideoIdOrderByCreatedAtAscIdAsc(UUID videoId);

	Mono<CustodyEvent> findFirstByVideoIdOrderByCreatedAtDescIdDesc(UUID videoId);
}
