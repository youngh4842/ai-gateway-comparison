package com.example.mockllm.service;

import com.example.mockllm.dto.ChatCompletionRequest;
import com.example.mockllm.dto.ChatCompletionResponse;
import com.example.mockllm.dto.ChoiceDto;
import com.example.mockllm.dto.MessageDto;
import com.example.mockllm.dto.UsageDto;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 실제 AI 호출 없이 상담 요약 테스트용 고정 응답을 만듭니다.
 */
@Service
public class MockLlmService {

    private static final String FIXED_RESULT = "{\"summary\":\"고객이 배송 지연에 대해 문의했고 당일 연락을 요청했습니다.\",\"categoryCode\":\"DELIVERY_DELAY\",\"categoryName\":\"배송지연\",\"riskLevel\":\"NORMAL\"}";

    public ChatCompletionResponse complete(ChatCompletionRequest request) {
        String userContent = request.messages().stream()
                .filter(message -> "user".equals(message.role()))
                .map(MessageDto::content)
                .findFirst()
                .orElse("");

        // TODO: 실제 내부 LLM 연동 시 userContent를 프롬프트 또는 모델 입력으로 전달합니다.
        return new ChatCompletionResponse(
                "mock-chatcmpl-001",
                "chat.completion",
                "internal-counsel-mock-llm",
                List.of(new ChoiceDto(0, new MessageDto("assistant", FIXED_RESULT), "stop")),
                new UsageDto(100, 50, 150)
        );
    }
}
