package com.example.cs.service;

import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PrivacyGuardService {

    private static final Pattern MOBILE_PHONE_PATTERN = Pattern.compile("(?<!\\d)(01[016789])[- ]?(\\d{4})[- ]?(\\d{4})(?!\\d)");

    public PrivacyGuardResult apply(String text, PrivacyPolicy policy) {
        String source = text == null ? "" : text;
        Matcher matcher = MOBILE_PHONE_PATTERN.matcher(source);
        if (!matcher.find()) {
            return new PrivacyGuardResult(source, false, false, "NO_PII_DETECTED");
        }

        if (policy == PrivacyPolicy.BLOCK) {
            return new PrivacyGuardResult(source, true, true, "PHONE_NUMBER_BLOCKED");
        }

        if (policy == PrivacyPolicy.OFF) {
            return new PrivacyGuardResult(source, true, false, "PHONE_NUMBER_DETECTED_BUT_ALLOWED");
        }

        String masked = matcher.replaceAll("$1-****-$3");
        return new PrivacyGuardResult(masked, true, false, "PHONE_NUMBER_MASKED");
    }

    public PrivacyPolicy normalizePolicy(String value, PrivacyPolicy defaultPolicy) {
        if (value == null || value.isBlank()) {
            return defaultPolicy;
        }
        try {
            return PrivacyPolicy.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultPolicy;
        }
    }

    public enum PrivacyPolicy {
        MASK,
        BLOCK,
        OFF
    }

    public record PrivacyGuardResult(
            String safeText,
            boolean detected,
            boolean blocked,
            String decision
    ) {
    }
}
