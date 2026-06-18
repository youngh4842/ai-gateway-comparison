package com.example.mockllm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 실제 LLM 대신 고정 응답을 반환하는 내부 Mock LLM 애플리케이션입니다.
 */
@SpringBootApplication
public class MockLlmApplication {

    public static void main(String[] args) {
        SpringApplication.run(MockLlmApplication.class, args);
    }
}
