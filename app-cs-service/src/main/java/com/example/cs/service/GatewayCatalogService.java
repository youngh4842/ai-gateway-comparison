package com.example.cs.service;

import com.example.cs.dto.GatewayOption;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GatewayCatalogService {

    private final List<GatewayOption> options = List.of(
            new GatewayOption(
                    "apisix",
                    "Apache APISIX",
                    "무료",
                    "현재 연결됨",
                    true,
                    "http://gateway:9080",
                    "/ai/v1/chat/completions",
                    "현재 APISIX 컨테이너로 실제 호출됩니다."
            ),
            new GatewayOption(
                    "envoy",
                    "Envoy",
                    "무료 self-host",
                    "현재 연결됨",
                    true,
                    "http://gateway-envoy:9080",
                    "/ai/v1/chat/completions",
                    "Envoy 컨테이너로 실제 호출됩니다. Lua 필터로 demo API key와 rate-limit 케이스를 흉내냅니다."
            ),
            new GatewayOption(
                    "litellm",
                    "LiteLLM Proxy",
                    "무료 self-host",
                    "현재 연결됨",
                    true,
                    "http://gateway-litellm:4000",
                    "/v1/chat/completions",
                    "LiteLLM Proxy 컨테이너로 실제 호출됩니다. Mock LLM을 OpenAI-compatible provider로 등록했습니다."
            ),
            new GatewayOption(
                    "portkey",
                    "Portkey AI Gateway",
                    "무료 self-host",
                    "후보",
                    false,
                    "",
                    "/v1/chat/completions",
                    "오픈소스 AI Gateway가 있으나 현재 PoC compose에는 연결하지 않았습니다."
            ),
            new GatewayOption(
                    "cloudflare",
                    "Cloudflare AI Gateway",
                    "무료 플랜, 외부 계정 필요",
                    "표시만",
                    false,
                    "",
                    "Cloudflare endpoint",
                    "Cloudflare 계정과 외부 endpoint 설정이 필요하므로 로컬 기본 PoC에는 붙이지 않았습니다."
            ),
            new GatewayOption(
                    "openrouter",
                    "OpenRouter",
                    "무료 모델 가능, API 키 필요",
                    "표시만",
                    false,
                    "",
                    "https://openrouter.ai/api/v1/chat/completions",
                    "무료 모델은 선택 가능하지만 외부 API 키와 모델별 제한이 있어 로컬 기본 PoC에는 붙이지 않았습니다."
            ),
            new GatewayOption(
                    "kong",
                    "Kong Gateway",
                    "유료/라이선스 주의",
                    "표시만",
                    false,
                    "",
                    "/ai/v1/chat/completions",
                    "최신 Kong Gateway Enterprise free mode deprecation 이슈가 있어 무료 PoC 기본 후보에서는 제외했습니다."
            )
    );

    public List<GatewayOption> options() {
        return options;
    }

    public Optional<GatewayOption> find(String id) {
        return options.stream()
                .filter(option -> option.id().equalsIgnoreCase(id))
                .findFirst();
    }
}
