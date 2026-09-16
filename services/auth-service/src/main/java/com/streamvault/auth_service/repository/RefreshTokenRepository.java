package com.streamvault.auth_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import com.streamvault.auth_service.entity.RefreshToken;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, UUID> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);
}
