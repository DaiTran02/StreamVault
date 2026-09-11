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
@Table("videos")
public class Video implements Persistable<UUID> {

	@Id
	private UUID id;

	@Column("original_filename")
	private String originalFilename;

	@Column("content_type")
	private String contentType;

	@Column("size_bytes")
	private long sizeBytes;

	@Column("s3_bucket")
	private String s3Bucket;

	@Column("s3_key")
	private String s3Key;

	private VideoStatus status;

	@Column("error_message")
	private String errorMessage;

	@Column("created_at")
	private Instant createdAt;

	@Column("updated_at")
	private Instant updatedAt;

	@Transient
	@Builder.Default
	private boolean isNew = true;

	@Override
	public boolean isNew() {
		return isNew;
	}
}
