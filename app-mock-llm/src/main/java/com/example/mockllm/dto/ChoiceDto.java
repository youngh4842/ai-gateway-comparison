package com.example.mockllm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Chat Completions 응답의 choices 항목입니다.
 */
public record ChoiceDto(
        Integer index,
        MessageDto message,
        @JsonProperty("finish_reason") String finishReason
) {
}
