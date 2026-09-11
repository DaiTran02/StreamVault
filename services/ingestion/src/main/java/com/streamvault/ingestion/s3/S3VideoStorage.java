package com.streamvault.ingestion.s3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;

import com.streamvault.ingestion.config.AwsProperties;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class S3VideoStorage {

	private static final Logger log = LoggerFactory.getLogger(S3VideoStorage.class);

	private final S3AsyncClient s3;
	private final String bucket;

	public S3VideoStorage(S3AsyncClient s3, AwsProperties awsProperties) {
		this.s3 = s3;
		this.bucket = awsProperties.s3().bucket();
	}

	public String bucket() {
		return bucket;
	}

	public Mono<Long> put(String key, String contentType, FilePart file) {
		return Mono.fromCallable(() -> Files.createTempFile("streamvault-upload-", ".bin"))
				.subscribeOn(Schedulers.boundedElastic())
				.flatMap(temp -> file.transferTo(temp)
						.then(Mono.fromCallable(() -> Files.size(temp)))
						.flatMap(sizeBytes -> upload(key, contentType, temp, sizeBytes))
						.doFinally(signal -> deleteQuietly(temp)));
	}

	private Mono<Long> upload(String key, String contentType, Path temp, long sizeBytes) {
		PutObjectRequest request = PutObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.contentType(contentType)
				.contentLength(sizeBytes)
				.build();
		return Mono.fromFuture(() -> s3.putObject(request, AsyncRequestBody.fromFile(temp)))
				.thenReturn(sizeBytes);
	}

	private static void deleteQuietly(Path temp) {
		try {
			Files.deleteIfExists(temp);
		}
		catch (IOException ex) {
			log.warn("Could not delete temp upload file {}", temp, ex);
		}
	}
}
