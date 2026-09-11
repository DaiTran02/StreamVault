package com.streamvault.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws")
public record AwsProperties(String region, S3 s3) {

	public record S3(
			String bucket,
			String endpoint,
			boolean pathStyleEnabled,
			String accessKey,
			String secretKey) {
	}
}
