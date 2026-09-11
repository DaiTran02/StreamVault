package com.streamvault.ingestion.config;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;

@Configuration
public class S3Config {

	@Bean(destroyMethod = "close")
	S3AsyncClient s3AsyncClient(AwsProperties awsProperties) {
		AwsProperties.S3 s3 = awsProperties.s3();
		S3AsyncClientBuilder builder = S3AsyncClient.builder()
				.region(Region.of(awsProperties.region()))
				.httpClientBuilder(NettyNioAsyncHttpClient.builder())
				.requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
				.responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED);

		if (StringUtils.hasText(s3.endpoint())) {
			builder.endpointOverride(URI.create(s3.endpoint()))
					.forcePathStyle(true);
		} else if (s3.pathStyleEnabled()) {
			builder.forcePathStyle(true);
		}
		if (StringUtils.hasText(s3.accessKey()) && StringUtils.hasText(s3.secretKey())) {
			builder.credentialsProvider(StaticCredentialsProvider.create(
					AwsBasicCredentials.create(s3.accessKey(), s3.secretKey())));
		}
		return builder.build();
	}
}
