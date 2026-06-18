package com.example.mockllm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * OpenAI Chat Completions API와 유사한 요청 DTO입니다.
 */
public record ChatCompletionRequest(
        @NotBlank String model,
        @Valid @NotEmpty List<MessageDto> messages,
        Double temperature
) {
}
