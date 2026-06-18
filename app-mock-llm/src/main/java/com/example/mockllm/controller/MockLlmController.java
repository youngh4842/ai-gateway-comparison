package com.example.mockllm.controller;

import com.example.mockllm.dto.ChatCompletionRequest;
import com.example.mockllm.dto.ChatCompletionResponse;
import com.example.mockllm.service.MockLlmService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내부 LLM처럼 보이는 Chat Completions API를 제공합니다.
 */
@RestController
@RequestMapping("/v1/chat")
public class MockLlmController {

    private final MockLlmService mockLlmService;

    public MockLlmController(MockLlmService mockLlmService) {
        this.mockLlmService = mockLlmService;
    }

    @PostMapping("/completions")
    public ChatCompletionResponse complete(@Valid @RequestBody ChatCompletionRequest request) {
        return mockLlmService.complete(request);
    }
}
