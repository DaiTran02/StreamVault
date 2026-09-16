package com.streamvault.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank
		@Size(min = 3, max = 64)
		@Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "username may contain letters, digits, '.', '_' and '-'")
		String username,
		@NotBlank
		@Size(min = 8, max = 128)
		String password) {
}
