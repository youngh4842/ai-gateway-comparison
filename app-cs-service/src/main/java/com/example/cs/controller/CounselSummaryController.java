package com.example.cs.controller;

import com.example.cs.dto.CounselSummaryRequest;
import com.example.cs.dto.CounselSummaryResponse;
import com.example.cs.service.CounselSummaryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 상담 내용 요약 API를 제공하는 CS 서비스의 진입점입니다.
 */
@RestController
@RequestMapping("/counsels")
public class CounselSummaryController {

    private final CounselSummaryService counselSummaryService;

    public CounselSummaryController(CounselSummaryService counselSummaryService) {
        this.counselSummaryService = counselSummaryService;
    }

    @PostMapping("/summary")
    public CounselSummaryResponse summarize(@Valid @RequestBody CounselSummaryRequest request) {
        return counselSummaryService.summarize(request);
    }
}
