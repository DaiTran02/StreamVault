package com.streamvault.ingestion.dto;

import java.time.Instant;
import java.util.UUID;

import com.streamvault.ingestion.entity.CustodyEvent;

public record CustodyEventResponse(
		UUID id,
		UUID videoId,
		UUID userId,
		String action,
		String contentSha256,
		String previousChainHash,
		String chainHash,
		Instant createdAt) {

	public static CustodyEventResponse from(CustodyEvent event) {
		return new CustodyEventResponse(
				event.getId(),
				event.getVideoId(),
				event.getUserId(),
				event.getAction(),
				event.getContentSha256(),
				event.getPreviousChainHash(),
				event.getChainHash(),
				event.getCreatedAt());
	}
}
