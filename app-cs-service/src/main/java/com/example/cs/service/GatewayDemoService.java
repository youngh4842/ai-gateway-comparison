package com.example.cs.service;

import com.example.cs.config.AiGatewayProperties;
import com.example.cs.dto.ChatCompletionRequest;
import com.example.cs.dto.ChatCompletionResponse;
import com.example.cs.dto.CounselSummaryResponse;
import com.example.cs.dto.GatewayCompareResponse;
import com.example.cs.dto.GatewayDemoRequest;
import com.example.cs.dto.GatewayDemoResponse;
import com.example.cs.dto.GatewayOption;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs a single visual demo request through the active gateway and records each hop.
 */
@Service
public class GatewayDemoService {

    private final WebClient webClient;
    private final AiGatewayProperties properties;
    private final ObjectMapper objectMapper;
    private final GatewayCatalogService gatewayCatalogService;
    private final PrivacyGuardService privacyGuardService;

    public GatewayDemoService(WebClient webClient, AiGatewayProperties properties, ObjectMapper objectMapper, GatewayCatalogService gatewayCatalogService, PrivacyGuardService privacyGuardService) {
        this.webClient = webClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.gatewayCatalogService = gatewayCatalogService;
        this.privacyGuardService = privacyGuardService;
    }

    public GatewayDemoResponse run(GatewayDemoRequest request) {
        GatewayOption selectedGateway = gatewayCatalogService.find(normalizeGatewayId(request.gatewayId()))
                .orElseGet(() -> gatewayCatalogService.find("apisix").orElseThrow());
        if (!selectedGateway.runnable()) {
            return unavailableGatewayResponse(request, selectedGateway);
        }

        String scenario = normalizeScenario(request.scenario());
        String path = scenarioPath(scenario, request.gatewayPath(), selectedGateway);
        String apiKey = scenarioApiKey(scenario, request.apiKey(), selectedGateway);
        String gatewayUrl = selectedGateway.baseUrl() + path;
        List<String> events = new ArrayList<>();
        PrivacyGuardService.PrivacyPolicy privacyPolicy = privacyGuardService.normalizePolicy(
                request.privacyPolicy(),
                PrivacyGuardService.PrivacyPolicy.MASK
        );
        PrivacyGuardService.PrivacyGuardResult privacy = privacyGuardService.apply(request.counselText(), privacyPolicy);

        events.add("Service accepted counsel text.");
        events.add("Privacy guard decision: " + privacy.decision() + ".");
        if (privacy.detected()) {
            events.add("Service detected an 11-digit phone number before calling the gateway.");
        }
        if (privacy.blocked()) {
            events.add("Service blocked the request before the gateway was reached.");
            return privacyBlockedResponse(request, scenario, path, gatewayUrl, apiKey, events);
        }
        events.add("Selected gateway: " + selectedGateway.name() + ".");
        events.add("Service sent request to " + selectedGateway.name() + ", not directly to Mock LLM.");

        ChatCompletionRequest chatRequest = new ChatCompletionRequest(
                properties.model(),
                List.of(
                        new ChatCompletionRequest.Message("system", "Summarize and classify commerce CS counsel."),
                        new ChatCompletionRequest.Message("user", privacy.safeText())
                ),
                0.0
        );

        GatewayCallResult callResult;
        if ("RATE_LIMIT".equals(scenario)) {
            GatewayCallResult firstCall = callGateway(gatewayUrl, apiKey, chatRequest);
            events.add("Rate-limit demo sent request #1 to consume the demo quota.");
            events.add("Request #1 status: " + firstCall.statusCode() + ".");
            callResult = callGateway(gatewayUrl, apiKey, chatRequest);
            events.add("Rate-limit demo sent request #2 to prove the gateway can block overflow traffic.");
        } else {
            callResult = callGateway(gatewayUrl, apiKey, chatRequest);
        }

        if (callResult == null) {
            callResult = new GatewayCallResult(502, "");
        }

        boolean passed = callResult.statusCode() >= 200 && callResult.statusCode() < 300;
        CounselSummaryResponse summary = passed ? parseSummary(callResult.body()) : null;

        if (passed) {
            events.add("Gateway accepted the API key and matched the route.");
            events.add("Gateway rewrote the path and forwarded the request to Mock LLM.");
            events.add("Mock LLM returned a fixed summary JSON.");
            events.add("Service converted the LLM message into the final response.");
            addSuccessScenarioEvents(scenario, events);
        } else if (callResult.statusCode() == 401) {
            events.add("Gateway blocked the request before Mock LLM was reached.");
        } else if (callResult.statusCode() == 404) {
            events.add("Gateway did not find a matching route.");
        } else if (callResult.statusCode() == 429) {
            events.add("Gateway rate limit blocked the request before Mock LLM was reached.");
        } else {
            events.add("Gateway request failed before a final counsel summary was created.");
        }

        return new GatewayDemoResponse(
                scenario,
                new GatewayDemoResponse.ServiceStep(
                        request.counselText(),
                        gatewayUrl,
                        apiKey.isBlank() ? "(empty)" : apiKey,
                        properties.systemCode(),
                        properties.taskType()
                ),
                new GatewayDemoResponse.GatewayStep(
                        true,
                        passed,
                        callResult.statusCode(),
                        gatewayDecision(callResult.statusCode()),
                        path
                ),
                new GatewayDemoResponse.LlmStep(
                        passed,
                        "/v1/chat/completions",
                        passed ? privacy.safeText() : ""
                ),
                summary,
                callResult.body(),
                events
        );
    }

    public GatewayCompareResponse compare(GatewayDemoRequest request) {
        String scenario = normalizeScenario(request.scenario());
        List<GatewayCompareResponse.GatewayCompareItem> results = gatewayCatalogService.options().stream()
                .filter(GatewayOption::runnable)
                .map(option -> compareOne(request, option))
                .toList();
        return new GatewayCompareResponse(scenario, results);
    }

    private GatewayCompareResponse.GatewayCompareItem compareOne(GatewayDemoRequest request, GatewayOption option) {
        GatewayDemoRequest gatewayRequest = new GatewayDemoRequest(
                request.counselText(),
                option.id(),
                request.privacyPolicy(),
                request.apiKey(),
                option.routePath(),
                request.scenario()
        );
        long started = System.nanoTime();
        GatewayDemoResponse response = run(gatewayRequest);
        long elapsedMs = Math.max(1, (System.nanoTime() - started) / 1_000_000);
        return new GatewayCompareResponse.GatewayCompareItem(
                option.id(),
                option.name(),
                option.costLabel(),
                response.service().targetGatewayUrl(),
                elapsedMs,
                response.gateway().statusCode(),
                response.gateway().decision(),
                response.gateway().received(),
                response.gateway().passed(),
                response.llm().reached(),
                response.gateway().routePath(),
                preview(response.rawGatewayResponse())
        );
    }

    private String preview(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.length() <= 180 ? value : value.substring(0, 180) + "...";
    }

    private GatewayDemoResponse privacyBlockedResponse(
            GatewayDemoRequest request,
            String scenario,
            String path,
            String gatewayUrl,
            String apiKey,
            List<String> events
    ) {
        return new GatewayDemoResponse(
                scenario,
                new GatewayDemoResponse.ServiceStep(
                        request.counselText(),
                        gatewayUrl,
                        apiKey.isBlank() ? "(empty)" : apiKey,
                        properties.systemCode(),
                        properties.taskType()
                ),
                new GatewayDemoResponse.GatewayStep(
                        false,
                        false,
                        0,
                        "BLOCKED_BY_PRIVACY_GUARD",
                        path
                ),
                new GatewayDemoResponse.LlmStep(
                        false,
                        "/v1/chat/completions",
                        ""
                ),
                null,
                "Request blocked by app-cs-service privacy guard before gateway call.",
                events
        );
    }

    private GatewayCallResult callGateway(String gatewayUrl, String apiKey, ChatCompletionRequest chatRequest) {
        return webClient.post()
                .uri(gatewayUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-KEY", apiKey)
                .header("Authorization", "Bearer " + apiKey)
                .header("X-System-Code", properties.systemCode())
                .header("X-Task-Type", properties.taskType())
                .bodyValue(chatRequest)
                .exchangeToMono(response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> new GatewayCallResult(response.statusCode().value(), body)))
                .block();
    }

    private CounselSummaryResponse parseSummary(String body) {
        try {
            ChatCompletionResponse chatResponse = objectMapper.readValue(body, ChatCompletionResponse.class);
            String content = chatResponse.choices().get(0).message().content();
            return objectMapper.readValue(content, CounselSummaryResponse.class);
        } catch (JsonProcessingException | RuntimeException e) {
            return null;
        }
    }

    private String gatewayDecision(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) {
            return "PASSED";
        }
        if (statusCode == 401) {
            return "BLOCKED_BY_KEY_AUTH";
        }
        if (statusCode == 404) {
            return "ROUTE_NOT_FOUND";
        }
        if (statusCode == 429) {
            return "RATE_LIMITED";
        }
        return "FAILED";
    }

    private String normalizePath(String gatewayPath) {
        if (gatewayPath == null || gatewayPath.isBlank()) {
            return "/ai/v1/chat/completions";
        }
        return gatewayPath.startsWith("/") ? gatewayPath : "/" + gatewayPath;
    }

    private String normalizeScenario(String scenario) {
        if (scenario == null || scenario.isBlank()) {
            return "CUSTOM";
        }
        return scenario.trim().toUpperCase();
    }

    private String normalizeGatewayId(String gatewayId) {
        if (gatewayId == null || gatewayId.isBlank()) {
            return "apisix";
        }
        return gatewayId.trim().toLowerCase();
    }

    private GatewayDemoResponse unavailableGatewayResponse(GatewayDemoRequest request, GatewayOption selectedGateway) {
        String scenario = normalizeScenario(request.scenario());
        String path = normalizePath(request.gatewayPath());
        List<String> events = new ArrayList<>();
        events.add("Selected gateway: " + selectedGateway.name() + ".");
        events.add("This gateway is listed for comparison, but it is not connected to the current local compose stack.");
        events.add("Cost status: " + selectedGateway.costLabel() + ".");
        events.add(selectedGateway.notes());

        return new GatewayDemoResponse(
                scenario,
                new GatewayDemoResponse.ServiceStep(
                        request.counselText(),
                        selectedGateway.routePath(),
                        "(not sent)",
                        properties.systemCode(),
                        properties.taskType()
                ),
                new GatewayDemoResponse.GatewayStep(
                        false,
                        false,
                        0,
                        "NOT_CONNECTED",
                        path
                ),
                new GatewayDemoResponse.LlmStep(
                        false,
                        "/v1/chat/completions",
                        ""
                ),
                null,
                selectedGateway.notes(),
                events
        );
    }

    private String scenarioPath(String scenario, String requestedPath, GatewayOption selectedGateway) {
        if ("litellm".equalsIgnoreCase(selectedGateway.id())) {
            return selectedGateway.routePath();
        }
        return switch (scenario) {
            case "ROUTING", "AUTH_BLOCK", "SERVER_HIDING", "OBSERVABILITY" -> "/ai/v1/chat/completions";
            case "RATE_LIMIT" -> "/ai/v1/chat/completions-limited";
            case "WRONG_ROUTE" -> "/ai/v1/wrong";
            default -> normalizePath(requestedPath);
        };
    }

    private String scenarioApiKey(String scenario, String requestedApiKey, GatewayOption selectedGateway) {
        if ("litellm".equalsIgnoreCase(selectedGateway.id())) {
            return "AUTH_BLOCK".equals(scenario) ? "" : "sk-test-api-key";
        }
        return switch (scenario) {
            case "AUTH_BLOCK" -> "";
            case "ROUTING", "RATE_LIMIT", "SERVER_HIDING", "OBSERVABILITY", "WRONG_ROUTE" -> properties.apiKey();
            default -> requestedApiKey == null ? "" : requestedApiKey;
        };
    }

    private void addSuccessScenarioEvents(String scenario, List<String> events) {
        if ("ROUTING".equals(scenario)) {
            events.add("Routing case passed: /ai/v1/chat/completions reached Mock LLM through the gateway.");
        }
        if ("SERVER_HIDING".equals(scenario)) {
            events.add("Server hiding case passed: Service used only the gateway URL and did not call app-mock-llm directly.");
        }
        if ("OBSERVABILITY".equals(scenario)) {
            events.add("Observability case passed: Gateway logs can record caller, API path, status, upstream, and timing.");
        }
    }

    private record GatewayCallResult(int statusCode, String body) {
    }
}
