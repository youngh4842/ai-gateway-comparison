package com.example.cs.dto;

public record GatewayOption(
        String id,
        String name,
        String costLabel,
        String availability,
        boolean runnable,
        String baseUrl,
        String routePath,
        String notes
) {
}
