package com.streamvault.ingestion.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;

import com.streamvault.ingestion.exception.VideoAccessDeniedException;

public final class UserIds {

	private UserIds() {
	}

	public static UUID from(Authentication authentication) {
		if (authentication == null || authentication.getName() == null) {
			throw new VideoAccessDeniedException();
		}
		try {
			return UUID.fromString(authentication.getName());
		}
		catch (IllegalArgumentException ex) {
			throw new VideoAccessDeniedException();
		}
	}
}
