package com.rxs.library.security;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.rxs.library.common.Result;
import com.rxs.library.exception.LibraryErrorCode;
import com.rxs.library.i18n.LocalizedMessages;

import lombok.RequiredArgsConstructor;

/** 无状态 Bearer 认证、签名校验和公开资源访问策略。 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final LocalizedMessages messages;
    private static final int MINIMUM_JWT_KEY_BYTES = 32;

    @Bean
    SecretKey jwtKey(@Value("${app.jwt-secret}") String encoded) {
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            if (bytes.length < MINIMUM_JWT_KEY_BYTES) {
                throw new IllegalArgumentException();
            }
            return new SecretKeySpec(bytes, "HmacSHA256");
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "JWT_SECRET must be Base64 containing at least 32 random bytes");
        }
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key, @Value("${app.jwt-issuer}") String issuer, Clock clock) {
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator timestamps = new JwtTimestampValidator(Duration.ZERO);
        timestamps.setClock(clock);
        OAuth2TokenValidator<Jwt> claims =
                jwt -> {
                    try {
                        if (Long.parseLong(jwt.getSubject()) <= 0
                                || jwt.getIssuedAt() == null
                                || jwt.getExpiresAt() == null
                                || jwt.getIssuedAt().isAfter(clock.instant())
                                || !jwt.getExpiresAt().isAfter(jwt.getIssuedAt())) {
                            throw new IllegalArgumentException();
                        }
                        return OAuth2TokenValidatorResult.success();
                    } catch (RuntimeException ex) {
                        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
                    }
                };
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        timestamps, new JwtIssuerValidator(issuer), claims));
        return decoder;
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, ObjectMapper mapper) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/IAuthBiz/register",
                                                "/IAuthBiz/login",
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html",
                                                "/error")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/IBookBiz/queryBook/page",
                                                "/IBookBiz/queryBook")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/",
                                                "/index.html",
                                                "/assets/**",
                                                "/favicon.svg")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        errors ->
                                errors.authenticationEntryPoint(
                                                (req, res, ex) ->
                                                        writeProblem(
                                                                mapper,
                                                                res,
                                                                LibraryErrorCode
                                                                        .AUTHENTICATION_REQUIRED))
                                        .accessDeniedHandler(
                                                (req, res, ex) ->
                                                        writeProblem(
                                                                mapper,
                                                                res,
                                                                LibraryErrorCode.FORBIDDEN)))
                .oauth2ResourceServer(
                        oauth ->
                                oauth.jwt(jwt -> {})
                                        .authenticationEntryPoint(
                                                (req, res, ex) ->
                                                        writeProblem(
                                                                mapper,
                                                                res,
                                                                LibraryErrorCode.INVALID_TOKEN)));
        return http.build();
    }

    private void writeProblem(
            ObjectMapper mapper, HttpServletResponse res, LibraryErrorCode errorCode)
            throws IOException {
        res.setStatus(errorCode.getStatus().value());
        if (errorCode.getStatus().value() == 401) {
            res.setHeader("WWW-Authenticate", "Bearer");
        }
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        mapper.writeValue(
                res.getOutputStream(), Result.failure(errorCode, messages.text(errorCode)));
    }
}
