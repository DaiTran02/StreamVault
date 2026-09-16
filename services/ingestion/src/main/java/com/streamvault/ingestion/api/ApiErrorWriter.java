package com.streamvault.ingestion.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.streamvault.ingestion.dto.ApiResponse;

import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class ApiErrorWriter {

	private final ObjectMapper objectMapper;
	private final String serviceName;

	public ApiErrorWriter(ObjectMapper objectMapper, @Value("${spring.application.name:ingestion}") String serviceName) {
		this.objectMapper = objectMapper;
		this.serviceName = serviceName;
	}

	public Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String message) {
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(status);
		response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		byte[] bytes;
		try {
			bytes = objectMapper.writeValueAsBytes(ApiResponse.error(status.value(), serviceName, message));
		}
		catch (JacksonException ex) {
			return Mono.error(ex);
		}
		return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
	}
}
