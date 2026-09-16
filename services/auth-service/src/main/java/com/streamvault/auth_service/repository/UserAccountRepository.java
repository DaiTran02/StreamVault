package com.streamvault.auth_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import com.streamvault.auth_service.entity.UserAccount;

public interface UserAccountRepository extends CrudRepository<UserAccount, UUID> {

	Optional<UserAccount> findByUsername(String username);

	boolean existsByUsername(String username);
}
