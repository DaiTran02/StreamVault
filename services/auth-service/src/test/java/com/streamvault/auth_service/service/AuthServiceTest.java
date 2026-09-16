package com.streamvault.auth_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.streamvault.auth_service.dto.LoginRequest;
import com.streamvault.auth_service.dto.RegisterRequest;
import com.streamvault.auth_service.dto.TokenResponse;
import com.streamvault.auth_service.entity.UserAccount;
import com.streamvault.auth_service.exception.AuthException;
import com.streamvault.auth_service.repository.RefreshTokenRepository;
import com.streamvault.auth_service.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserAccountRepository users;

	@Mock
	private RefreshTokenRepository refreshTokens;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private TokenService tokenService;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(users, refreshTokens, passwordEncoder, tokenService);
	}

	@Test
	void registerIssuesToken() {
		when(users.existsByUsername("alice")).thenReturn(false);
		when(passwordEncoder.encode("password1")).thenReturn("hash");
		when(users.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
		TokenResponse tokens = token("alice");
		when(tokenService.issue(any(UserAccount.class))).thenReturn(tokens);

		TokenResponse result = authService.register(new RegisterRequest("alice", "password1"));

		assertEquals("alice", result.username());
		assertEquals("access", result.accessToken());
		verify(users).save(any(UserAccount.class));
	}

	@Test
	void registerRejectsDuplicateUsername() {
		when(users.existsByUsername("alice")).thenReturn(true);

		AuthException ex = assertThrows(AuthException.class,
				() -> authService.register(new RegisterRequest("alice", "password1")));
		assertEquals(HttpStatus.CONFLICT, ex.getStatus());
	}

	@Test
	void loginRejectsBadPassword() {
		UserAccount user = UserAccount.builder()
				.id(UUID.randomUUID())
				.username("alice")
				.passwordHash("hash")
				.enabled(true)
				.createdAt(Instant.now())
				.build();
		when(users.findByUsername("alice")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

		AuthException ex = assertThrows(AuthException.class,
				() -> authService.login(new LoginRequest("alice", "wrong")));
		assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
	}

	private static TokenResponse token(String username) {
		return new TokenResponse("access", "Bearer", 3600, "refresh", TokenService.SCOPE, UUID.randomUUID(), username);
	}
}
