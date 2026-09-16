package com.streamvault.ingestion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import com.streamvault.ingestion.api.ApiErrorWriter;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

	@Bean
	SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, ApiErrorWriter apiErrorWriter) {
		return http
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.authorizeExchange(exchanges -> exchanges
						.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.anyExchange().authenticated())
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint((exchange, ex) ->
								apiErrorWriter.write(exchange, HttpStatus.UNAUTHORIZED, "unauthorized"))
						.accessDeniedHandler((exchange, ex) ->
								apiErrorWriter.write(exchange, HttpStatus.FORBIDDEN, "forbidden")))
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(Customizer.withDefaults())
						.authenticationEntryPoint((exchange, ex) ->
								apiErrorWriter.write(exchange, HttpStatus.UNAUTHORIZED, "unauthorized")))
				.build();
	}
}
