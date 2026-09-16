package com.streamvault.ingestion.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.streamvault.ingestion.custody.Sha256;
import com.streamvault.ingestion.entity.CustodyEvent;
import com.streamvault.ingestion.exception.CustodyIntegrityException;
import com.streamvault.ingestion.repository.CustodyEventRepository;

import reactor.core.publisher.Mono;

@Service
public class ChainOfCustodyService {

	public static final String ACTION_INGEST = "INGEST";
	public static final String ACTION_DETECTED = "DETECTED";
	public static final String ACTION_ACCESS = "ACCESS";

	private final CustodyEventRepository repository;

	public ChainOfCustodyService(CustodyEventRepository repository) {
		this.repository = repository;
	}

	public Mono<CustodyEvent> record(UUID videoId, UUID userId, String action, String contentSha256) {
		Instant createdAt = Instant.now();
		return repository.findFirstByVideoIdOrderByCreatedAtDescIdDesc(videoId)
				.map(CustodyEvent::getChainHash)
				.defaultIfEmpty(Sha256.GENESIS)
				.flatMap(previous -> {
					CustodyEvent event = CustodyEvent.builder()
							.id(UUID.randomUUID())
							.videoId(videoId)
							.userId(userId)
							.action(action)
							.contentSha256(contentSha256)
							.previousChainHash(previous)
							.chainHash(Sha256.chainHash(
									previous, videoId, userId, action, contentSha256, createdAt.toEpochMilli()))
							.createdAt(createdAt)
							.build();
					return repository.save(event);
				});
	}

	public Mono<Void> verify(UUID videoId) {
		return list(videoId).flatMap(events -> {
			if (events.isEmpty()) {
				return Mono.error(new CustodyIntegrityException(videoId));
			}
			String previous = Sha256.GENESIS;
			for (CustodyEvent event : events) {
				long epochMilli = event.getCreatedAt().toEpochMilli();
				String expected = Sha256.chainHash(
						previous,
						event.getVideoId(),
						event.getUserId(),
						event.getAction(),
						event.getContentSha256(),
						epochMilli);
				if (!previous.equals(event.getPreviousChainHash()) || !expected.equals(event.getChainHash())) {
					return Mono.error(new CustodyIntegrityException(videoId));
				}
				previous = event.getChainHash();
			}
			return Mono.empty();
		});
	}

	public Mono<List<CustodyEvent>> list(UUID videoId) {
		return repository.findByVideoIdOrderByCreatedAtAscIdAsc(videoId).collectList();
	}
}
