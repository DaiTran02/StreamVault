package com.streamvault.auth_service.api;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.streamvault.auth_service.dto.ApiResponse;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Component
public class ApiErrorWriter {

	private final ObjectMapper objectMapper;
	private final String serviceName;

	public ApiErrorWriter(ObjectMapper objectMapper, @Value("${spring.application.name:auth-service}") String serviceName) {
		this.objectMapper = objectMapper;
		this.serviceName = serviceName;
	}

	public void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(status.value(), serviceName, message));
	}
}
