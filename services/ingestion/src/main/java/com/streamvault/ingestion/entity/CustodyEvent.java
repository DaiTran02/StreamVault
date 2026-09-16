package com.streamvault.ingestion.entity;

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
@Table("custody_events")
public class CustodyEvent implements Persistable<UUID> {

	@Id
	private UUID id;

	@Column("video_id")
	private UUID videoId;

	@Column("user_id")
	private UUID userId;

	private String action;

	@Column("content_sha256")
	private String contentSha256;

	@Column("previous_chain_hash")
	private String previousChainHash;

	@Column("chain_hash")
	private String chainHash;

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
