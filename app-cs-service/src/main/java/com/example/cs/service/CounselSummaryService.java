package com.example.cs.service;

import com.example.cs.client.AiGatewayClient;
import com.example.cs.config.AiGatewayProperties;
import com.example.cs.dto.ChatCompletionRequest;
import com.example.cs.dto.CounselSummaryRequest;
import com.example.cs.dto.CounselSummaryResponse;
import com.example.cs.exception.PrivacyGuardException;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.example.cs.service.PrivacyGuardService.PrivacyPolicy.MASK;

/**
 * 상담 내용을 OpenAI Chat Completions 스타일 요청으로 바꾸고 결과 DTO로 변환합니다.
 */
@Service
public class CounselSummaryService {

    private final AiGatewayClient aiGatewayClient;
    private final AiGatewayProperties properties;
    private final PrivacyGuardService privacyGuardService;

    public CounselSummaryService(AiGatewayClient aiGatewayClient, AiGatewayProperties properties, PrivacyGuardService privacyGuardService) {
        this.aiGatewayClient = aiGatewayClient;
        this.properties = properties;
        this.privacyGuardService = privacyGuardService;
    }

    public CounselSummaryResponse summarize(CounselSummaryRequest request) {
        PrivacyGuardService.PrivacyGuardResult privacy = privacyGuardService.apply(request.counselText(), MASK);
        if (privacy.blocked()) {
            throw new PrivacyGuardException("개인정보가 포함되어 AI Gateway 호출이 차단되었습니다.");
        }

        ChatCompletionRequest chatRequest = new ChatCompletionRequest(
                properties.model(),
                List.of(
                        new ChatCompletionRequest.Message(
                                "system",
                                "너는 쇼핑몰 CS 상담 내용을 요약하고 분류하는 내부 LLM이다."
                        ),
                        new ChatCompletionRequest.Message("user", privacy.safeText())
                ),
                0.0
        );

        return aiGatewayClient.requestCounselSummary(chatRequest);
    }
}
