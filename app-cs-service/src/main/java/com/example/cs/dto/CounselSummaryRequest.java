package com.example.cs.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * CS 사용자가 입력한 상담 원문입니다.
 */
public record CounselSummaryRequest(
        @NotBlank String counselText
) {
}
