package com.example.mockllm.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Chat Completions messages 배열의 단일 메시지를 표현합니다.
 */
public record MessageDto(
        @NotBlank String role,
        @NotBlank String content
) {
}
