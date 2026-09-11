package com.streamvault.ingestion.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.streamvault.ingestion.config.KafkaProperties;

import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

@Component
public class VideoUploadedPublisher {

	private final KafkaSender<String, String> kafkaSender;
	private final ObjectMapper objectMapper;
	private final String topic;

	public VideoUploadedPublisher(
			KafkaSender<String, String> kafkaSender,
			ObjectMapper objectMapper,
			KafkaProperties kafkaProperties) {
		this.kafkaSender = kafkaSender;
		this.objectMapper = objectMapper;
		this.topic = kafkaProperties.topic().videoUploaded();
	}

	public Mono<Void> publish(VideoUploadedEvent event) {
		String payload;
		try {
			payload = objectMapper.writeValueAsString(event);
		}
		catch (JacksonException ex) {
			return Mono.error(ex);
		}
		ProducerRecord<String, String> record = new ProducerRecord<>(topic, event.videoId().toString(), payload);
		return kafkaSender.send(Mono.just(SenderRecord.create(record, event.videoId())))
				.then();
	}
}
