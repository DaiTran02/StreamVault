package com.streamvault.auth_service.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.streamvault.auth_service.dto.LoginRequest;
import com.streamvault.auth_service.dto.RegisterRequest;
import com.streamvault.auth_service.dto.TokenResponse;
import com.streamvault.auth_service.entity.RefreshToken;
import com.streamvault.auth_service.entity.UserAccount;
import com.streamvault.auth_service.exception.AuthException;
import com.streamvault.auth_service.repository.RefreshTokenRepository;
import com.streamvault.auth_service.repository.UserAccountRepository;

@Service
public class AuthService {

	private final UserAccountRepository users;
	private final RefreshTokenRepository refreshTokens;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;

	public AuthService(
			UserAccountRepository users,
			RefreshTokenRepository refreshTokens,
			PasswordEncoder passwordEncoder,
			TokenService tokenService) {
		this.users = users;
		this.refreshTokens = refreshTokens;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
	}

	@Transactional
	public TokenResponse register(RegisterRequest request) {
		String username = request.username().trim();
		if (users.existsByUsername(username)) {
			throw new AuthException(HttpStatus.CONFLICT, "username already exists");
		}
		UserAccount user = UserAccount.builder()
				.id(UUID.randomUUID())
				.username(username)
				.passwordHash(passwordEncoder.encode(request.password()))
				.enabled(true)
				.createdAt(Instant.now())
				.build();
		return tokenService.issue(users.save(user));
	}

	@Transactional
	public TokenResponse login(LoginRequest request) {
		UserAccount user = users.findByUsername(request.username().trim())
				.filter(UserAccount::isEnabled)
				.filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
				.orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "invalid credentials"));
		return tokenService.issue(user);
	}

	@Transactional
	public TokenResponse refresh(String refreshToken) {
		String hash = tokenService.sha256(refreshToken);
		RefreshToken stored = refreshTokens.findByTokenHash(hash)
				.orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "invalid refresh token"));
		if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
			throw new AuthException(HttpStatus.UNAUTHORIZED, "invalid refresh token");
		}
		stored.setRevoked(true);
		stored.setNew(false);
		refreshTokens.save(stored);
		UserAccount user = users.findById(stored.getUserId())
				.filter(UserAccount::isEnabled)
				.orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "invalid refresh token"));
		return tokenService.issue(user);
	}

	public UserAccount requireUser(UUID userId) {
		return users.findById(userId)
				.filter(UserAccount::isEnabled)
				.orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "user not found"));
	}
}
