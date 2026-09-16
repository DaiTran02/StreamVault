package com.streamvault.auth_service.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.streamvault.auth_service.api.ApiErrorWriter;
import com.streamvault.auth_service.entity.UserAccount;
import com.streamvault.auth_service.repository.UserAccountRepository;
import com.streamvault.auth_service.service.TokenService;

@Configuration
@EnableWebSecurity
public class AuthorizationServerConfig {

	@Bean
	@Order(1)
	SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
		OAuth2AuthorizationServerConfigurer authorizationServer = new OAuth2AuthorizationServerConfigurer();
		http.securityMatcher(authorizationServer.getEndpointsMatcher())
				.with(authorizationServer, configurer -> configurer.oidc(Customizer.withDefaults()))
				.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
				.exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
						(request, response, authException) -> response.sendError(401),
						new MediaTypeRequestMatcher(MediaType.APPLICATION_JSON)));
		return http.build();
	}

	@Bean
	@Order(2)
	SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, ApiErrorWriter apiErrorWriter) throws Exception {
		http.securityMatcher("/api/**")
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh")
						.permitAll()
						.anyRequest()
						.authenticated())
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint((request, response, ex) ->
								apiErrorWriter.write(response, HttpStatus.UNAUTHORIZED, "unauthorized"))
						.accessDeniedHandler((request, response, ex) ->
								apiErrorWriter.write(response, HttpStatus.FORBIDDEN, "forbidden")))
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(Customizer.withDefaults())
						.authenticationEntryPoint((request, response, ex) ->
								apiErrorWriter.write(response, HttpStatus.UNAUTHORIZED, "unauthorized")));
		return http.build();
	}

	@Bean
	RegisteredClientRepository registeredClientRepository(AuthProperties properties, PasswordEncoder passwordEncoder) {
		RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
				.clientId(properties.clientId())
				.clientSecret(passwordEncoder.encode(properties.clientSecret()))
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.redirectUri("http://localhost:8080/login/oauth2/code/streamvault")
				.postLogoutRedirectUri("http://localhost:8080/")
				.scope(OidcScopes.OPENID)
				.scope(OidcScopes.PROFILE)
				.scope("videos.read")
				.scope("videos.write")
				.clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
				.tokenSettings(TokenSettings.builder()
						.accessTokenTimeToLive(properties.accessTokenTtl())
						.refreshTokenTimeToLive(properties.refreshTokenTtl())
						.reuseRefreshTokens(false)
						.build())
				.build();
		return new InMemoryRegisteredClientRepository(client);
	}

	@Bean
	JWKSource<SecurityContext> jwkSource() {
		RSAKey rsaKey = generateRsa();
		return new ImmutableJWKSet<>(new JWKSet(rsaKey));
	}

	@Bean
	JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
		return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
	}

	@Bean
	JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
		return new NimbusJwtEncoder(jwkSource);
	}

	@Bean
	AuthorizationServerSettings authorizationServerSettings(AuthProperties properties) {
		return AuthorizationServerSettings.builder().issuer(properties.issuer()).build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer(UserAccountRepository users) {
		return context -> {
			if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
				return;
			}
			String username = context.getPrincipal().getName();
			users.findByUsername(username).ifPresent(user -> applyUserClaims(context, user));
		};
	}

	private static void applyUserClaims(JwtEncodingContext context, UserAccount user) {
		context.getClaims().subject(user.getId().toString());
		context.getClaims().claim("username", user.getUsername());
		context.getClaims().claim("scope", TokenService.SCOPE);
	}

	private static RSAKey generateRsa() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			KeyPair keyPair = generator.generateKeyPair();
			return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
					.privateKey((RSAPrivateKey) keyPair.getPrivate())
					.keyID(UUID.randomUUID().toString())
					.build();
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to generate RSA key pair", ex);
		}
	}
}
