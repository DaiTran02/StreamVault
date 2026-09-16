package com.streamvault.auth_service.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streamvault.auth_service.dto.TokenResponse;
import com.streamvault.auth_service.config.AuthProperties;
import com.streamvault.auth_service.entity.RefreshToken;
import com.streamvault.auth_service.entity.UserAccount;
import com.streamvault.auth_service.repository.RefreshTokenRepository;

@Service
public class TokenService {

	public static final String SCOPE = "videos.read videos.write";

	private final JwtEncoder jwtEncoder;
	private final AuthProperties properties;
	private final RefreshTokenRepository refreshTokenRepository;
	private final SecureRandom secureRandom = new SecureRandom();

	public TokenService(
			JwtEncoder jwtEncoder,
			AuthProperties properties,
			RefreshTokenRepository refreshTokenRepository) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
		this.refreshTokenRepository = refreshTokenRepository;
	}

	@Transactional
	public TokenResponse issue(UserAccount user) {
		Instant now = Instant.now();
		Instant accessExpires = now.plus(properties.accessTokenTtl());
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(properties.issuer())
				.issuedAt(now)
				.expiresAt(accessExpires)
				.subject(user.getId().toString())
				.claim("username", user.getUsername())
				.claim("scope", SCOPE)
				.build();
		String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
		String refreshToken = newRefreshToken();
		refreshTokenRepository.save(RefreshToken.builder()
				.id(UUID.randomUUID())
				.userId(user.getId())
				.tokenHash(sha256(refreshToken))
				.expiresAt(now.plus(properties.refreshTokenTtl()))
				.revoked(false)
				.createdAt(now)
				.build());
		return new TokenResponse(
				accessToken,
				"Bearer",
				properties.accessTokenTtl().toSeconds(),
				refreshToken,
				SCOPE,
				user.getId(),
				user.getUsername());
	}

	public String sha256(String value) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 unavailable", ex);
		}
	}

	private String newRefreshToken() {
		byte[] bytes = new byte[32];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
