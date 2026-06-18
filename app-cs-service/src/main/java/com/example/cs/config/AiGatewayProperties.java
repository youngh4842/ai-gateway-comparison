package com.example.cs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Stores the AI gateway endpoint and demo headers used by the CS service.
 */
@ConfigurationProperties(prefix = "ai.gateway")
public record AiGatewayProperties(
        String url,
        String apiKey,
        String systemCode,
        String taskType,
        String model
) {
}
