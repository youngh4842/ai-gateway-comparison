package com.example.cs.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request used by the visual demo screen to test gateway pass/fail cases.
 */
public record GatewayDemoRequest(
        @NotBlank String counselText,
        String gatewayId,
        String privacyPolicy,
        String apiKey,
        String gatewayPath,
        String scenario
) {
}
