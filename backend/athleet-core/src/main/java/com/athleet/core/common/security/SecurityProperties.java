package com.athleet.core.common.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

/**
 * Security settings. Secrets are injected from environment variables, never committed.
 *
 * @param jwtSecret      base64 HMAC key, at least 256 bits
 * @param refreshCookieSecure false only for plain-http local development
 */
@Validated
@ConfigurationProperties("athleet.security")
public record SecurityProperties(
        @NotBlank String jwtSecret,
        @NotBlank String jwtIssuer,
        @NotNull Duration accessTokenTtl,
        @NotNull Duration refreshTokenTtl,
        @NotNull Duration passwordTokenTtl,
        boolean refreshCookieSecure,
        List<String> corsAllowedOrigins,
        @NotBlank String webBaseUrl) {
}
