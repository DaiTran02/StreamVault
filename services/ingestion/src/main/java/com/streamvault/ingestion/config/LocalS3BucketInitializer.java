package com.streamvault.ingestion.config;

import java.util.concurrent.CompletionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
public class LocalS3BucketInitializer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(LocalS3BucketInitializer.class);

	private final S3AsyncClient s3;
	private final AwsProperties awsProperties;

	public LocalS3BucketInitializer(S3AsyncClient s3, AwsProperties awsProperties) {
		this.s3 = s3;
		this.awsProperties = awsProperties;
	}

	@Override
	public void run(ApplicationArguments args) {
		AwsProperties.S3 s3Props = awsProperties.s3();
		if (!StringUtils.hasText(s3Props.endpoint()) || !StringUtils.hasText(s3Props.bucket())) {
			return;
		}
		String bucket = s3Props.bucket();
		try {
			s3.headBucket(HeadBucketRequest.builder().bucket(bucket).build()).join();
			log.info("S3 bucket {} already exists at {}", bucket, s3Props.endpoint());
		}
		catch (CompletionException ex) {
			if (!isMissingBucket(ex.getCause())) {
				throw ex;
			}
			s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build()).join();
			log.info("Created S3 bucket {} at {}", bucket, s3Props.endpoint());
		}
	}

	private static boolean isMissingBucket(Throwable cause) {
		if (cause instanceof NoSuchBucketException) {
			return true;
		}
		return cause instanceof S3Exception s3 && s3.statusCode() == 404;
	}
}
