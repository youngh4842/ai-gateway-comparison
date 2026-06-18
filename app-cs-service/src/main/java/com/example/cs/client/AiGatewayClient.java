package com.example.cs.client;

import com.example.cs.config.AiGatewayProperties;
import com.example.cs.dto.ChatCompletionRequest;
import com.example.cs.dto.ChatCompletionResponse;
import com.example.cs.dto.CounselSummaryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * AI HTTP client that calls the configured gateway instead of the internal LLM directly.
 */
@Component
public class AiGatewayClient {

    private final WebClient webClient;
    private final AiGatewayProperties properties;
    private final ObjectMapper objectMapper;

    public AiGatewayClient(WebClient webClient, AiGatewayProperties properties, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public CounselSummaryResponse requestCounselSummary(ChatCompletionRequest request) {
        ChatCompletionResponse response = webClient.post()
                .uri(properties.url())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-KEY", properties.apiKey())
                .header("X-System-Code", properties.systemCode())
                .header("X-Task-Type", properties.taskType())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatCompletionResponse.class)
                .block();

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("AI Gateway 응답에 choices가 없습니다.");
        }

        String content = response.choices().get(0).message().content();
        try {
            return objectMapper.readValue(content, CounselSummaryResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("AI Gateway 응답 content를 상담 요약 DTO로 변환할 수 없습니다.", e);
        }
    }
}
