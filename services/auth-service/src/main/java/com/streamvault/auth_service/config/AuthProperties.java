package com.streamvault.auth_service.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
		String issuer,
		Duration accessTokenTtl,
		Duration refreshTokenTtl,
		String clientId,
		String clientSecret) {
}
