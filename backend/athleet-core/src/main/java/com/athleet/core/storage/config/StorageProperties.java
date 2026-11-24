package com.athleet.core.storage.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * File storage settings, injected from environment variables.
 *
 * @param provider         {@code none} (uploads disabled) or {@code local}; S3 is added later
 * @param localRoot        folder for the local provider, outside any web root
 * @param urlSigningSecret HMAC key for download URLs, at least 32 bytes (required when the provider is not {@code none})
 * @param downloadUrlTtl   how long a signed download URL stays valid
 */
@Validated
@ConfigurationProperties("athleet.storage")
public record StorageProperties(
        @NotBlank String provider,
        String localRoot,
        String urlSigningSecret,
        @NotNull Duration downloadUrlTtl) {
}
