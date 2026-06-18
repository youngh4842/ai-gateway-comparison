package com.example.cs.dto;

import java.util.List;

/**
 * Response used by the visual demo screen to show each hop in the gateway flow.
 */
public record GatewayDemoResponse(
        String scenario,
        ServiceStep service,
        GatewayStep gateway,
        LlmStep llm,
        CounselSummaryResponse finalResult,
        String rawGatewayResponse,
        List<String> events
) {
    public record ServiceStep(
            String counselText,
            String targetGatewayUrl,
            String apiKeyLabel,
            String systemCode,
            String taskType
    ) {
    }

    public record GatewayStep(
            boolean received,
            boolean passed,
            int statusCode,
            String decision,
            String routePath
    ) {
    }

    public record LlmStep(
            boolean reached,
            String endpoint,
            String receivedUserContent
    ) {
    }
}
