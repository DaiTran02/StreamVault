package com.streamvault.auth_service.api;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.streamvault.auth_service.dto.ApiResponse;
import com.streamvault.auth_service.dto.LoginRequest;
import com.streamvault.auth_service.dto.RefreshRequest;
import com.streamvault.auth_service.dto.RegisterRequest;
import com.streamvault.auth_service.dto.TokenResponse;
import com.streamvault.auth_service.dto.UserResponse;
import com.streamvault.auth_service.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;
	private final String serviceName;

	public AuthController(AuthService authService, @Value("${spring.application.name:auth-service}") String serviceName) {
		this.authService = authService;
		this.serviceName = serviceName;
	}

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<TokenResponse>> register(@Valid @RequestBody RegisterRequest request) {
		return ApiResponse.created(serviceName, authService.register(request));
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.ok(serviceName, authService.login(request));
	}

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
		return ApiResponse.ok(serviceName, authService.refresh(request.refreshToken()));
	}

	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserResponse>> me(Authentication authentication) {
		UUID userId = UUID.fromString(authentication.getName());
		return ApiResponse.ok(serviceName, UserResponse.from(authService.requireUser(userId)));
	}
}
