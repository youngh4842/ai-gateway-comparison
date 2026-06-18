package com.example.cs.dto;

/**
 * 상담 요약과 분류 결과를 CS 서비스 응답 형식으로 표현합니다.
 */
public record CounselSummaryResponse(
        String summary,
        String categoryCode,
        String categoryName,
        String riskLevel
) {
}
