package com.streamvault.ingestion.entity;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
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
@Table("detections")
public class Detection {

	@Id
	private UUID id;

	@Column("video_id")
	private UUID videoId;

	private String label;

	private double confidence;

	@Column("bbox_x")
	private double bboxX;

	@Column("bbox_y")
	private double bboxY;

	@Column("bbox_w")
	private double bboxW;

	@Column("bbox_h")
	private double bboxH;

	@Column("frame_index")
	private int frameIndex;

	@Column("ts_ms")
	private long tsMs;

	@Column("created_at")
	private Instant createdAt;
}
