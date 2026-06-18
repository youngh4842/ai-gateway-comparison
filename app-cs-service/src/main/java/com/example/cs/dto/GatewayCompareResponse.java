package com.example.cs.dto;

import java.util.List;

public record GatewayCompareResponse(
        String scenario,
        List<GatewayCompareItem> results
) {
    public record GatewayCompareItem(
            String gatewayId,
            String gatewayName,
            String costLabel,
            String targetGatewayUrl,
            long elapsedMs,
            int statusCode,
            String decision,
            boolean gatewayReached,
            boolean gatewayPassed,
            boolean llmReached,
            String routePath,
            String rawGatewayResponsePreview
    ) {
    }
}
