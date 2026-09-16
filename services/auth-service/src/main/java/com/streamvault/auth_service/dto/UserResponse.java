package com.streamvault.auth_service.dto;

import java.time.Instant;
import java.util.UUID;

import com.streamvault.auth_service.entity.UserAccount;

public record UserResponse(UUID id, String username, Instant createdAt) {

	public static UserResponse from(UserAccount user) {
		return new UserResponse(user.getId(), user.getUsername(), user.getCreatedAt());
	}
}
