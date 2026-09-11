package com.streamvault.ingestion.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

@Configuration
public class KafkaConfig {

	@Bean(destroyMethod = "close")
	KafkaSender<String, String> kafkaSender(KafkaProperties kafkaProperties) {
		Map<String, Object> props = new HashMap<>();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.bootstrapServers());
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.ACKS_CONFIG, "all");
		props.put(ProducerConfig.CLIENT_ID_CONFIG, "streamvault-ingestion");

		KafkaProperties.Security security = kafkaProperties.security();
		if (security != null && StringUtils.hasText(security.protocol())) {
			props.put("security.protocol", security.protocol());
		}
		if (security != null && StringUtils.hasText(security.saslMechanism())) {
			props.put("sasl.mechanism", security.saslMechanism());
		}
		if (security != null && StringUtils.hasText(security.saslJaasConfig())) {
			props.put("sasl.jaas.config", security.saslJaasConfig());
		}

		return KafkaSender.create(SenderOptions.create(props));
	}
}
