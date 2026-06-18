package com.example.cs;

import com.example.cs.config.AiGatewayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 쇼핑몰 CS 업무 시스템 역할의 Spring Boot 애플리케이션입니다.
 */
@SpringBootApplication
@EnableConfigurationProperties(AiGatewayProperties.class)
public class CsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CsServiceApplication.class, args);
    }
}
