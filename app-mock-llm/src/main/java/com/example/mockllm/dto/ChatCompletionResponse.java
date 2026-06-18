package com.example.mockllm.dto;

import java.util.List;

/**
 * OpenAI Chat Completions API와 유사한 응답 DTO입니다.
 */
public record ChatCompletionResponse(
        String id,
        String object,
        String model,
        List<ChoiceDto> choices,
        UsageDto usage
) {
}
