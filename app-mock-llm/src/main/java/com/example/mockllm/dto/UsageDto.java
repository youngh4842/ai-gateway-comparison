package com.example.mockllm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 토큰 사용량처럼 보이는 Mock 메타데이터입니다.
 */
public record UsageDto(
        @JsonProperty("prompt_tokens") Integer promptTokens,
        @JsonProperty("completion_tokens") Integer completionTokens,
        @JsonProperty("total_tokens") Integer totalTokens
) {
}
