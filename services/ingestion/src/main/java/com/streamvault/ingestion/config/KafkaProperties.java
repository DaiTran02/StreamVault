package com.streamvault.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka")
public record KafkaProperties(String bootstrapServers, Topic topic, Security security) {

	public record Topic(String videoUploaded) {
	}

	public record Security(String protocol, String saslMechanism, String saslJaasConfig) {
	}
}
