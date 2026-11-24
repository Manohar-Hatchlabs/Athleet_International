package com.athleet.core.storage.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    public StorageConfig(StorageProperties props) {
        if (!"none".equals(props.provider())) {
            String secret = props.urlSigningSecret();
            if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalStateException("athleet.storage.url-signing-secret must be at least 32 bytes when storage is enabled");
            }
        }
    }
}
