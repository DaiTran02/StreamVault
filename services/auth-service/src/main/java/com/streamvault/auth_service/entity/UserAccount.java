package com.streamvault.auth_service.entity;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class UserAccount implements Persistable<UUID> {

	@Id
	private UUID id;

	private String username;

	@Column("password_hash")
	private String passwordHash;

	private boolean enabled;

	@Column("created_at")
	private Instant createdAt;

	@Transient
	@Builder.Default
	private boolean isNew = true;

	@Override
	public boolean isNew() {
		return isNew;
	}
}
