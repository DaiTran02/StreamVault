package com.streamvault.ingestion.service;

import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.streamvault.ingestion.custody.Sha256;
import com.streamvault.ingestion.entity.CustodyEvent;
import com.streamvault.ingestion.exception.CustodyIntegrityException;
import com.streamvault.ingestion.repository.CustodyEventRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ChainOfCustodyServiceTest {

	@Mock
	private CustodyEventRepository repository;

	@InjectMocks
	private ChainOfCustodyService service;

	@Test
	void verifyAcceptsValidChain() {
		UUID videoId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
		String content = "ab".repeat(32);
		String chain = Sha256.chainHash(Sha256.GENESIS, videoId, userId, "INGEST", content, createdAt.toEpochMilli());
		CustodyEvent event = CustodyEvent.builder()
				.id(UUID.randomUUID())
				.videoId(videoId)
				.userId(userId)
				.action("INGEST")
				.contentSha256(content)
				.previousChainHash(Sha256.GENESIS)
				.chainHash(chain)
				.createdAt(createdAt)
				.build();
		when(repository.findByVideoIdOrderByCreatedAtAscIdAsc(videoId)).thenReturn(Flux.just(event));

		StepVerifier.create(service.verify(videoId)).verifyComplete();
	}

	@Test
	void verifyRejectsTamperedHash() {
		UUID videoId = UUID.randomUUID();
		CustodyEvent event = CustodyEvent.builder()
				.id(UUID.randomUUID())
				.videoId(videoId)
				.userId(UUID.randomUUID())
				.action("INGEST")
				.contentSha256("ab".repeat(32))
				.previousChainHash(Sha256.GENESIS)
				.chainHash("deadbeef")
				.createdAt(Instant.parse("2026-01-01T00:00:00Z"))
				.build();
		when(repository.findByVideoIdOrderByCreatedAtAscIdAsc(videoId)).thenReturn(Flux.just(event));

		StepVerifier.create(service.verify(videoId)).expectError(CustodyIntegrityException.class).verify();
	}

	@Test
	void recordLinksToPreviousHash() {
		UUID videoId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		CustodyEvent previous = CustodyEvent.builder()
				.chainHash("aa".repeat(32))
				.build();
		when(repository.findFirstByVideoIdOrderByCreatedAtDescIdDesc(videoId)).thenReturn(Mono.just(previous));
		when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

		StepVerifier.create(service.record(videoId, userId, "ACCESS", "bb".repeat(32)))
				.assertNext(event -> {
					assert event.getPreviousChainHash().equals("aa".repeat(32));
					assert event.getChainHash().length() == 64;
				})
				.verifyComplete();
	}
}
