package com.example.cs.controller;

import com.example.cs.exception.PrivacyGuardException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(PrivacyGuardException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handlePrivacyGuardException(PrivacyGuardException e) {
        return Map.of(
                "code", "PRIVACY_GUARD_BLOCKED",
                "message", e.getMessage()
        );
    }
}
