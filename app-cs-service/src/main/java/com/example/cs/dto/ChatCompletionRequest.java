package com.example.cs.dto;

import java.util.List;

/**
 * OpenAI Chat Completions API와 유사한 요청 DTO입니다.
 */
public record ChatCompletionRequest(
        String model,
        List<Message> messages,
        Double temperature
) {
    public record Message(
            String role,
            String content
    ) {
    }
}
