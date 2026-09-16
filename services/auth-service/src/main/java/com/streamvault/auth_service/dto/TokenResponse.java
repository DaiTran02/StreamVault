package com.streamvault.auth_service.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenResponse(
		@JsonProperty("access_token") String accessToken,
		@JsonProperty("token_type") String tokenType,
		@JsonProperty("expires_in") long expiresIn,
		@JsonProperty("refresh_token") String refreshToken,
		String scope,
		@JsonProperty("user_id") UUID userId,
		String username) {
}
