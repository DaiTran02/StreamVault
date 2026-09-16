package com.streamvault.ingestion.custody;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class Sha256Test {

	@Test
	void hashesUtf8CanonicalChain() {
		UUID videoId = UUID.fromString("11111111-1111-1111-1111-111111111111");
		UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
		String hash = Sha256.chainHash(Sha256.GENESIS, videoId, userId, "INGEST", "ab".repeat(32), 1_704_067_200_000L);
		assertEquals(64, hash.length());
		assertEquals(Sha256.hex(
				(Sha256.GENESIS + "|" + videoId + "|" + userId + "|INGEST|" + "ab".repeat(32) + "|1704067200000")
						.getBytes(StandardCharsets.UTF_8)),
				hash);
	}

	@Test
	void changingAnyLinkChangesHash() {
		UUID videoId = UUID.fromString("11111111-1111-1111-1111-111111111111");
		UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
		String a = Sha256.chainHash(Sha256.GENESIS, videoId, userId, "INGEST", "ab".repeat(32), 1L);
		String b = Sha256.chainHash(Sha256.GENESIS, videoId, userId, "ACCESS", "ab".repeat(32), 1L);
		assertNotEquals(a, b);
	}
}
